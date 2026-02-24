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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LoanRepaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: LoanRepaymentRepository,
) : BaseViewModel<LoanRepaymentUiState, LoanRepaymentEvent, LoanRepaymentAction>(
    initialState = LoanRepaymentUiState(),
) {

    private val args = savedStateHandle.toRoute<LoanRepaymentScreenRoute>()
    private val _loanDetailsState = MutableStateFlow(LoanDetails())
    val loanDetailsState = _loanDetailsState.asStateFlow()

    init {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            repaymentDate = currentEpochMillis(),
        )
        trySendAction(LoanRepaymentAction.CheckDatabaseLoanRepayment)
    }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    override fun handleAction(action: LoanRepaymentAction) {
        when (action) {
            is LoanRepaymentAction.LoadLoanRepaymentTemplate -> {
                loadLoanRepaymentTemplate()
            }
            is LoanRepaymentAction.CheckDatabaseLoanRepayment -> {
                checkDatabaseLoanRepaymentByLoanId()
            }
            is LoanRepaymentAction.SubmitPayment -> {
                submitPayment(action.request)
            }
        }
    }

    private fun loadLoanRepaymentTemplate() {
        viewModelScope.launch {
            summaryRepository.getLoanById(args.loanId).collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> {
                        _loanRepaymentUiState.value = LoanRepaymentUiState.ShowProgressbar
                    }

                    is DataState.Success -> {
                        val loanWithAssociations = dataState.data
                        if (loanWithAssociations == null) {
                            _loanRepaymentUiState.value = LoanRepaymentUiState.ShowError(
                                Res.string.feature_loan_profile_error_details_not_found,
                            )
                            return@collect
                        }
                        _loanDetailsState.value = _loanDetailsState.value.copy(
                            loanId = loanWithAssociations.id,
                            clientName = loanWithAssociations.clientName,
                            loanProductName = loanWithAssociations.loanProductName,
                            amountInArrears = loanWithAssociations.summary.totalOverdue,
                            loanAccountNumber = loanWithAssociations.accountNo,
                        )
                        checkDatabaseLoanRepaymentByLoanId()
                    }

                    is DataState.Error -> {
                        _loanRepaymentUiState.value = LoanRepaymentUiState.ShowError(
                            Res.string.feature_loan_profile_failed_to_load_loan,
                        )
                    }
                }
            }
        }
    }

    fun loadLoanRepaymentTemplate() {
        viewModelScope.launch {
            repository.getLoanRepayTemplate(args.loanId).collect { state ->
                when (state) {
                    is DataState.Error -> {
                        mutableStateFlow.value = mutableStateFlow.value.copy(
                            isLoading = false,
                            error = Res.string.feature_loan_failed_to_load_loan_repayment,
                        )
                    }
                    DataState.Loading -> {
                        mutableStateFlow.value = mutableStateFlow.value.copy(isLoading = true, error = null)
                    }
                    is DataState.Success -> {
                        mutableStateFlow.value = mutableStateFlow.value.copy(
                            isLoading = false,
                            error = null,
                            loanRepaymentTemplate = state.data ?: LoanRepaymentTemplateEntity(),
                        )
                    }
                }
            }
        }
    }

    private fun submitPayment(request: LoanRepaymentRequestEntity) {
        viewModelScope.launch {
            mutableStateFlow.value = mutableStateFlow.value.copy(isLoading = true, error = null)

            try {
                val loanRepaymentResponse = repository.submitPayment(arg.loanId, request)
                mutableStateFlow.value = mutableStateFlow.value.copy(isLoading = false)
                sendEvent(LoanRepaymentEvent.PaymentSubmittedSuccessfully(loanRepaymentResponse))
            } catch (e: Exception) {
                mutableStateFlow.value = mutableStateFlow.value.copy(
                    isLoading = false,
                    error = Res.string.feature_loan_payment_failed,
                )
            }
        }
    }

    private fun checkDatabaseLoanRepaymentByLoanId() {
        viewModelScope.launch {
            repository.getDatabaseLoanRepaymentByLoanId(args.loanId).collect { state ->
                when (state) {
                    is DataState.Error -> {
                        mutableStateFlow.value = mutableStateFlow.value.copy(
                            isLoading = false,
                            hasCheckedDatabase = true,
                            error = Res.string.feature_loan_failed_to_load_loan_repayment,
                        )
                    }
                    DataState.Loading -> {
                        mutableStateFlow.value = mutableStateFlow.value.copy(isLoading = true, error = null)
                    }
                    is DataState.Success -> {
                        val existsInDatabase = state.data != null
                        mutableStateFlow.value = mutableStateFlow.value.copy(
                            isLoading = false,
                            error = null,
                            hasCheckedDatabase = true,
                            loanRepaymentExistsInDatabase = existsInDatabase,
                        )
                        if (!existsInDatabase) {
                            trySendAction(LoanRepaymentAction.LoadLoanRepaymentTemplate)
                        }
                    }
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

data class LoanRepaymentUiState(
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val loanRepaymentTemplate: LoanRepaymentTemplateEntity? = null,
    val loanRepaymentExistsInDatabase: Boolean = false,
    val hasCheckedDatabase: Boolean = false,
    val showPaymentDetails: Boolean = false,
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

sealed interface LoanRepaymentEvent {
    data class PaymentSubmittedSuccessfully(val response: LoanRepaymentResponseEntity?) : LoanRepaymentEvent
}

sealed interface LoanRepaymentAction {
    data object LoadLoanRepaymentTemplate : LoanRepaymentAction
    data object CheckDatabaseLoanRepayment : LoanRepaymentAction
    data class SubmitPayment(val request: LoanRepaymentRequestEntity) : LoanRepaymentAction
}
