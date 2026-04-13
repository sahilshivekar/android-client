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
import androidclient.feature.loan.generated.resources.feature_loan_error_amount_can_not_be_empty
import androidclient.feature.loan.generated.resources.feature_loan_error_select_payment_type
import androidclient.feature.loan.generated.resources.feature_loan_failed_to_load_loan_repayment
import androidclient.feature.loan.generated.resources.feature_loan_profile_error_details_not_found
import androidclient.feature.loan.generated.resources.feature_loan_profile_failed_to_load_loan
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mifos.core.common.utils.DateHelper
import com.mifos.core.data.repository.LoanRepaymentRepository
import com.mifos.core.ui.util.BaseViewModel
import com.mifos.room.entities.accounts.loans.LoanRepaymentRequestEntity
import com.mifos.room.entities.templates.loans.LoanRepaymentTemplateEntity
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class LoanRepaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: LoanRepaymentRepository,
) : BaseViewModel<LoanRepaymentUiState, LoanRepaymentEvent, LoanRepaymentAction>(
    initialState = LoanRepaymentUiState(
        repaymentDate = Clock.System.now().toEpochMilliseconds(),
    ),
) {

    private val args = savedStateHandle.toRoute<LoanRepaymentScreenRoute>()

    init {
        if (args.loanAccountNumber.isEmpty()) {
            loadLoanDetails()
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
            checkDatabaseAndLoadTemplate()
        }
    }

    override fun handleAction(action: LoanRepaymentAction) {
        when (action) {
            is LoanRepaymentAction.RetryLoading -> retryLoading()
            is LoanRepaymentAction.ReviewPayment -> validateAndShowConfirmation()
            is LoanRepaymentAction.SubmitPayment -> submitPayment()
            is LoanRepaymentAction.DismissConfirmation -> {
                mutableStateFlow.update { it.copy(showConfirmationDialog = false) }
            }

            is LoanRepaymentAction.CancelClicked -> {
                sendEvent(LoanRepaymentEvent.NavigateBack)
            }

            is LoanRepaymentAction.PaymentSuccess -> {
                sendEvent(LoanRepaymentEvent.NavigateBack)
            }

            is LoanRepaymentAction.DismissDialog -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
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

    private fun retryLoading() {
        if (args.loanAccountNumber.isEmpty()) {
            loadLoanDetails()
        } else {
            checkDatabaseAndLoadTemplate()
        }
    }

    private fun loadLoanDetails() {
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(isLoading = true, error = null) }
            try {
                val loan = repository.getLoanById(args.loanId)
                if (loan == null) {
                    mutableStateFlow.update {
                        it.copy(
                            isLoading = false,
                            error = Res.string
                                .feature_loan_profile_error_details_not_found,
                        )
                    }
                    return@launch
                }
                mutableStateFlow.update {
                    it.copy(
                        loanId = loan.id,
                        clientName = loan.clientName,
                        loanProductName = loan.loanProductName,
                        amountInArrears = loan.summary.totalOverdue,
                        loanAccountNumber = loan.accountNo,
                    )
                }
                checkDatabaseAndLoadTemplateInternal()
            } catch (e: Exception) {
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

    private fun checkDatabaseAndLoadTemplate() {
        viewModelScope.launch {
            checkDatabaseAndLoadTemplateInternal()
        }
    }

    private suspend fun checkDatabaseAndLoadTemplateInternal() {
        mutableStateFlow.update { it.copy(isLoading = true, error = null) }
        try {
            val existingRepayment =
                repository.getDatabaseLoanRepaymentByLoanId(args.loanId)
            if (existingRepayment != null) {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        loanRepaymentExistsInDatabase = true,
                    )
                }
                return
            }
            loadTemplate()
        } catch (e: Exception) {
            mutableStateFlow.update {
                it.copy(
                    isLoading = false,
                    error = Res.string.feature_loan_failed_to_load_loan_repayment,
                )
            }
        }
    }

    private suspend fun loadTemplate() {
        try {
            val template = repository.getLoanRepayTemplate(args.loanId)
                ?: LoanRepaymentTemplateEntity()
            val baseAmount =
                (template.principalPortion ?: 0.0) + (template.interestPortion ?: 0.0)
            val feesAmount = template.feeChargesPortion ?: 0.0

            mutableStateFlow.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    loanRepaymentTemplate = template,
                    amount = if (baseAmount > 0.0) baseAmount.toString() else "",
                    fees = if (feesAmount > 0.0) feesAmount.toString() else "",
                )
            }
        } catch (e: Exception) {
            mutableStateFlow.update {
                it.copy(
                    isLoading = false,
                    error = Res.string.feature_loan_failed_to_load_loan_repayment,
                )
            }
        }
    }

    private fun submitPayment() {
        if (state.isLoading) return
        viewModelScope.launch {
            mutableStateFlow.update {
                it.copy(isLoading = true, showConfirmationDialog = false)
            }

            val s = state
            val request = LoanRepaymentRequestEntity(
                accountNumber = s.accountNumber.ifBlank { s.loanAccountNumber },
                paymentTypeId = s.paymentTypeId.toString(),
                dateFormat = "dd-MM-yyyy",
                locale = "en",
                transactionAmount = s.total.toString(),
                transactionDate = DateHelper.getDateAsStringFromLong(s.repaymentDate),
                externalId = s.externalId.ifBlank { null },
                checkNumber = s.chequeNumber.ifBlank { null },
                routingCode = s.routingCode.ifBlank { null },
                receiptNumber = s.receiptNumber.ifBlank { null },
                bankNumber = s.bankNumber.ifBlank { null },
                note = s.note.ifBlank { null },
            )

            try {
                val response = repository.submitPayment(args.loanId, request)
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        dialogState = LoanRepaymentUiState.DialogState.Success(
                            resourceId = response.resourceId,
                        ),
                    )
                }
            } catch (e: Exception) {
                mutableStateFlow.update {
                    it.copy(
                        isLoading = false,
                        dialogState = LoanRepaymentUiState.DialogState.Error(
                            message = e.message ?: "An error occurred",
                        ),
                    )
                }
            }
        }
    }

    private fun validateAndShowConfirmation() {
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
}

@Immutable
internal data class LoanRepaymentUiState(
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val dialogState: DialogState? = null,
    val loanRepaymentTemplate: LoanRepaymentTemplateEntity? = null,
    val loanRepaymentExistsInDatabase: Boolean = false,
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
) {
    val total: Double
        get() {
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            val additionalValue = additionalPayment.toDoubleOrNull() ?: 0.0
            val feesValue = fees.toDoubleOrNull() ?: 0.0
            val penaltiesValue = if (waivePenalties) {
                0.0
            } else {
                loanRepaymentTemplate?.penaltyChargesPortion ?: 0.0
            }
            return amountValue + additionalValue + feesValue + penaltiesValue
        }

    sealed interface DialogState {
        data class Success(val resourceId: Int?) : DialogState
        data class Error(val message: String) : DialogState
    }
}

internal sealed interface LoanRepaymentEvent {
    data object NavigateBack : LoanRepaymentEvent
}

internal sealed interface LoanRepaymentAction {
    data object RetryLoading : LoanRepaymentAction
    data object ReviewPayment : LoanRepaymentAction
    data object SubmitPayment : LoanRepaymentAction
    data object DismissConfirmation : LoanRepaymentAction
    data object CancelClicked : LoanRepaymentAction
    data object PaymentSuccess : LoanRepaymentAction
    data object DismissDialog : LoanRepaymentAction
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
