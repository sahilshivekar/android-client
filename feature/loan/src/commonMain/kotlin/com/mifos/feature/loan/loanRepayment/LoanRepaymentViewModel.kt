/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.loanRepayment

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_failed_to_load_loan_repayment
import androidclient.feature.loan.generated.resources.feature_loan_payment_failed
import androidclient.feature.loan.generated.resources.feature_loan_profile_error_details_not_found
import androidclient.feature.loan.generated.resources.feature_loan_profile_failed_to_load_loan
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mifos.core.common.utils.DataState
import com.mifos.core.data.repository.LoanAccountSummaryRepository
import com.mifos.core.data.repository.LoanRepaymentRepository
import com.mifos.core.ui.util.BaseViewModel
import com.mifos.room.entities.accounts.loans.LoanRepaymentRequestEntity
import com.mifos.room.entities.accounts.loans.LoanRepaymentResponseEntity
import com.mifos.room.entities.templates.loans.LoanRepaymentTemplateEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal class LoanRepaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: LoanRepaymentRepository,
    private val summaryRepository: LoanAccountSummaryRepository,
) : BaseViewModel<LoanRepaymentUiState, LoanRepaymentEvent, LoanRepaymentAction>(
    initialState = LoanRepaymentUiState(
        repaymentDate = Clock.System.now().toEpochMilliseconds(),
    ),
) {

    private val args = savedStateHandle.toRoute<LoanRepaymentScreenRoute>()
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        if (args.loanAccountNumber.isEmpty()) {
            loadLoanById()
        } else {
            mutableStateFlow.update {
                it.copy(
                    loanAccountNumber = args.loanAccountNumber,
                    loanId = args.loanId,
                    clientName = args.clientName,
                    loanProductName = args.loanProductName,
                    amountInArrears = args.amountInArrears,
                )
            }
            observeDatabaseAndTemplate()
        }
    }

    override fun handleAction(action: LoanRepaymentAction) {
        when (action) {
            is LoanRepaymentAction.LoadLoanRepaymentTemplate -> loadLoanRepaymentTemplate()
            is LoanRepaymentAction.CheckDatabaseLoanRepayment -> retryTrigger.tryEmit(Unit)
            is LoanRepaymentAction.SubmitPayment -> {
                mutableStateFlow.update { it.copy(showConfirmationDialog = false) }
                submitPayment(action.request)
            }
            is LoanRepaymentAction.ReviewPayment -> validateAndShowConfirmation()
            is LoanRepaymentAction.DismissConfirmation -> {
                mutableStateFlow.update { it.copy(showConfirmationDialog = false) }
            }
            is LoanRepaymentAction.OnPaymentTypeChange -> {
                mutableStateFlow.update { it.copy(paymentType = action.paymentType) }
            }
            is LoanRepaymentAction.OnPaymentTypeSelected -> {
                mutableStateFlow.update {
                    it.copy(
                        paymentType = action.paymentType,
                        paymentTypeId = action.paymentTypeId,
                        paymentTypeError = null,
                    )
                }
            }
            is LoanRepaymentAction.OnAmountChange -> {
                mutableStateFlow.update { it.copy(amount = action.amount, amountError = null) }
            }
            is LoanRepaymentAction.OnAdditionalPaymentChange -> {
                mutableStateFlow.update {
                    it.copy(additionalPayment = action.additionalPayment)
                }
            }
            is LoanRepaymentAction.OnFeesChange -> {
                mutableStateFlow.update { it.copy(fees = action.fees) }
            }
            is LoanRepaymentAction.OnRepaymentDateChange -> {
                mutableStateFlow.update { it.copy(repaymentDate = action.date) }
            }
            is LoanRepaymentAction.OnShowPaymentDetailsChange -> {
                mutableStateFlow.update { it.copy(showPaymentDetails = action.show) }
            }
            is LoanRepaymentAction.OnAccountNumberChange -> {
                mutableStateFlow.update { it.copy(accountNumber = action.accountNumber) }
            }
            is LoanRepaymentAction.OnExternalIdChange -> {
                mutableStateFlow.update { it.copy(externalId = action.externalId) }
            }
            is LoanRepaymentAction.OnChequeNumberChange -> {
                mutableStateFlow.update { it.copy(chequeNumber = action.chequeNumber) }
            }
            is LoanRepaymentAction.OnRoutingCodeChange -> {
                mutableStateFlow.update { it.copy(routingCode = action.routingCode) }
            }
            is LoanRepaymentAction.OnReceiptNumberChange -> {
                mutableStateFlow.update { it.copy(receiptNumber = action.receiptNumber) }
            }
            is LoanRepaymentAction.OnBankNumberChange -> {
                mutableStateFlow.update { it.copy(bankNumber = action.bankNumber) }
            }
            is LoanRepaymentAction.OnNoteChange -> {
                mutableStateFlow.update { it.copy(note = action.note) }
            }
            is LoanRepaymentAction.OnWaivePenaltiesChange -> {
                mutableStateFlow.update { it.copy(waivePenalties = action.waive) }
            }
        }
    }

    private fun loadLoanById() {
        viewModelScope.launch {
            summaryRepository.getLoanById(args.loanId).collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> {
                        mutableStateFlow.update { it.copy(isLoading = true, error = null) }
                    }

                    is DataState.Success -> {
                        val loan = dataState.data
                        if (loan == null) {
                            mutableStateFlow.update {
                                it.copy(
                                    isLoading = false,
                                    error = Res.string
                                        .feature_loan_profile_error_details_not_found,
                                )
                            }
                            return@collect
                        }
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                loanId = loan.id,
                                clientName = loan.clientName,
                                loanProductName = loan.loanProductName,
                                amountInArrears = loan.summary.totalOverdue,
                                loanAccountNumber = loan.accountNo,
                            )
                        }
                        observeDatabaseAndTemplate()
                    }

                    is DataState.Error -> {
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = false,
                                error = Res.string
                                    .feature_loan_profile_failed_to_load_loan,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeDatabaseAndTemplate() {
        viewModelScope.launch {
            retryTrigger
                .onStart { emit(Unit) }
                .flatMapLatest {
                    repository.getDatabaseLoanRepaymentByLoanId(args.loanId)
                }
                .collect { dbState ->
                    when (dbState) {
                        is DataState.Loading -> {
                            mutableStateFlow.update {
                                it.copy(isLoading = true, error = null)
                            }
                        }

                        is DataState.Error -> {
                            mutableStateFlow.update {
                                it.copy(
                                    isLoading = false,
                                    hasCheckedDatabase = true,
                                    error = Res.string
                                        .feature_loan_failed_to_load_loan_repayment,
                                )
                            }
                        }

                        is DataState.Success -> {
                            val existsInDatabase = dbState.data != null
                            mutableStateFlow.update {
                                it.copy(
                                    isLoading = false,
                                    error = null,
                                    hasCheckedDatabase = true,
                                    loanRepaymentExistsInDatabase = existsInDatabase,
                                )
                            }
                            if (!existsInDatabase) {
                                loadLoanRepaymentTemplate()
                            }
                        }
                    }
                }
        }
    }

    private fun loadLoanRepaymentTemplate() {
        viewModelScope.launch {
            repository.getLoanRepayTemplate(args.loanId).collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> {
                        mutableStateFlow.update {
                            it.copy(isLoading = true, error = null)
                        }
                    }

                    is DataState.Error -> {
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = false,
                                error = Res.string
                                    .feature_loan_failed_to_load_loan_repayment,
                            )
                        }
                    }

                    is DataState.Success -> {
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                loanRepaymentTemplate = dataState.data
                                    ?: LoanRepaymentTemplateEntity(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun submitPayment(request: LoanRepaymentRequestEntity) {
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(isLoading = true, error = null) }

            try {
                val response = repository.submitPayment(args.loanId, request)
                mutableStateFlow.update { it.copy(isLoading = false) }
                sendEvent(LoanRepaymentEvent.PaymentSubmittedSuccessfully(response))
            } catch (e: Exception) {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        error = Res.string.feature_loan_payment_failed,
                    )
                }
            }
        }
    }

    private fun checkDatabaseLoanRepaymentByLoanId() {
        viewModelScope.launch {
            val currentState = state
            val amountValid =
                currentState.amount.trim().toDoubleOrNull()?.let { it > 0 } == true
            val paymentTypeValid = currentState.paymentType.isNotBlank()

            if (amountValid && paymentTypeValid) {
                mutableStateFlow.update {
                    it.copy(
                        showConfirmationDialog = true,
                        amountError = null,
                        paymentTypeError = null,
                    )
                }
            } else {
                mutableStateFlow.update {
                    it.copy(
                        amountError = if (!amountValid) {
                            getString(
                                Res.string.feature_loan_error_amount_can_not_be_empty,
                            )
                        } else {
                            null
                        },
                        paymentTypeError = if (!paymentTypeValid) {
                            getString(
                                Res.string.feature_loan_error_select_payment_type,
                            )
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun updatePaymentType(paymentType: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(paymentType = paymentType)
    }

    fun updatePaymentTypeWithId(paymentType: String, paymentTypeId: Int) {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            paymentType = paymentType,
            paymentTypeId = paymentTypeId,
            paymentTypeError = null,
        )
    }

    fun updateAmount(amount: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            amount = amount,
            amountError = null,
        )
    }

    fun updateAdditionalPayment(additionalPayment: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(additionalPayment = additionalPayment)
    }

    fun updateFees(fees: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(fees = fees)
    }

    fun updateRepaymentDate(date: Long) {
        mutableStateFlow.value = mutableStateFlow.value.copy(repaymentDate = date)
    }

    fun updateShowPaymentDetails(show: Boolean) {
        mutableStateFlow.value = mutableStateFlow.value.copy(showPaymentDetails = show)
    }

    fun updateAccountNumber(accountNumber: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(accountNumber = accountNumber)
    }

    fun updateExternalId(externalId: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(externalId = externalId)
    }

    fun updateChequeNumber(chequeNumber: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(chequeNumber = chequeNumber)
    }

    fun updateRoutingCode(routingCode: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(routingCode = routingCode)
    }

    fun updateReceiptNumber(receiptNumber: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(receiptNumber = receiptNumber)
    }

    fun updateBankNumber(bankNumber: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(bankNumber = bankNumber)
    }

    fun updateNote(note: String) {
        mutableStateFlow.value = mutableStateFlow.value.copy(note = note)
    }

    fun updateWaivePenalties(waive: Boolean) {
        mutableStateFlow.value = mutableStateFlow.value.copy(waivePenalties = waive)
    }

    fun setValidationErrors(amountError: String?, paymentTypeError: String?) {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            amountError = amountError,
            paymentTypeError = paymentTypeError,
        )
    }
}

internal data class LoanRepaymentUiState(
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val loanRepaymentTemplate: LoanRepaymentTemplateEntity? = null,
    val loanRepaymentExistsInDatabase: Boolean = false,
    val hasCheckedDatabase: Boolean = false,
    val showPaymentDetails: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val clientName: String = "",
    val loanId: Int = 0,
    val loanAccountNumber: String = "",
    val loanProductName: String = "",
    val amountInArrears: Double? = 0.0,
    val paymentType: String = "",
    val amount: String = "",
    val additionalPayment: String = "",
    val fees: String = "",
    val paymentTypeId: Int = 0,
    val repaymentDate: Long = 0L,
    val accountNumber: String = "",
    val externalId: String = "",
    val chequeNumber: String = "",
    val routingCode: String = "",
    val receiptNumber: String = "",
    val bankNumber: String = "",
    val note: String = "",
    val waivePenalties: Boolean = false,
    val amountError: String? = null,
    val paymentTypeError: String? = null,
)

internal sealed interface LoanRepaymentEvent {
    data class PaymentSubmittedSuccessfully(
        val response: LoanRepaymentResponseEntity?,
    ) : LoanRepaymentEvent
}

internal sealed interface LoanRepaymentAction {
    data object LoadLoanRepaymentTemplate : LoanRepaymentAction
    data object CheckDatabaseLoanRepayment : LoanRepaymentAction
    data class SubmitPayment(val request: LoanRepaymentRequestEntity) : LoanRepaymentAction
    data class OnPaymentTypeChange(val paymentType: String) : LoanRepaymentAction
    data class OnPaymentTypeSelected(
        val paymentType: String,
        val paymentTypeId: Int,
    ) : LoanRepaymentAction
    data class OnAmountChange(val amount: String) : LoanRepaymentAction
    data class OnAdditionalPaymentChange(val additionalPayment: String) : LoanRepaymentAction
    data class OnFeesChange(val fees: String) : LoanRepaymentAction
    data class OnRepaymentDateChange(val date: Long) : LoanRepaymentAction
    data class OnShowPaymentDetailsChange(val show: Boolean) : LoanRepaymentAction
    data class OnAccountNumberChange(val accountNumber: String) : LoanRepaymentAction
    data class OnExternalIdChange(val externalId: String) : LoanRepaymentAction
    data class OnChequeNumberChange(val chequeNumber: String) : LoanRepaymentAction
    data class OnRoutingCodeChange(val routingCode: String) : LoanRepaymentAction
    data class OnReceiptNumberChange(val receiptNumber: String) : LoanRepaymentAction
    data class OnBankNumberChange(val bankNumber: String) : LoanRepaymentAction
    data class OnNoteChange(val note: String) : LoanRepaymentAction
    data class OnWaivePenaltiesChange(val waive: Boolean) : LoanRepaymentAction
}
