/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.loanAccountAction

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_action_error_details_not_found
import androidclient.feature.loan.generated.resources.feature_loan_action_failed_to_load_loam_actions
import androidclient.feature.loan.generated.resources.feature_loan_profile_error_network_not_available
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mifos.core.common.utils.DataState
import com.mifos.core.data.repository.LoanAccountSummaryRepository
import com.mifos.core.data.util.NetworkMonitor
import com.mifos.core.ui.util.BaseViewModel
import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

internal class LoanAccountActionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val networkMonitor: NetworkMonitor,
    private val loanRepository: LoanAccountSummaryRepository,
) : BaseViewModel<LoanAccountActionsState, LoanAccountActionsEvent, LoanAccountActionsAction>(
    initialState = LoanAccountActionsState(),
) {
    private val route = savedStateHandle.toRoute<LoanAccountActionRoute>()
    private var loadJob: Job? = null

    init {
        observeNetworkAndLoad()
    }

    private fun observeNetworkAndLoad() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isConnected ->
                mutableStateFlow.update { it.copy(networkConnection = isConnected) }
                if (isConnected) {
                    if (mutableStateFlow.value.actions.isEmpty()) {
                        loadLoanAccountDetails(route.loanId)
                    }
                } else if (mutableStateFlow.value.actions.isEmpty()) {
                    mutableStateFlow.update {
                        it.copy(
                            isLoading = false,
                            dialogState = LoanAccountActionsState.DialogState.Error(Res.string.feature_loan_profile_error_network_not_available),
                        )
                    }
                }
            }
        }
    }

    private fun loadLoanAccountDetails(loanId: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loanRepository.getLoanById(loanId).collect { result ->
                when (result) {
                    is DataState.Success -> {
                        val loan = result.data
                        if (loan == null) {
                            mutableStateFlow.update {
                                it.copy(
                                    isLoading = false,
                                    dialogState = LoanAccountActionsState.DialogState.Error(Res.string.feature_loan_action_error_details_not_found),
                                )
                            }
                            return@collect
                        }

                        val status = loan.status.value.toLoanStatus()
                        val flags = calculateFlags(loan, status)
                        generateActions(status, flags)
                    }
                    is DataState.Error -> {
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = false,
                                dialogState = LoanAccountActionsState.DialogState.Error(Res.string.feature_loan_action_failed_to_load_loam_actions),
                            )
                        }
                    }
                    DataState.Loading -> {
                        mutableStateFlow.update {
                            it.copy(
                                isLoading = true,
                                dialogState = LoanAccountActionsState.DialogState.Loading,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun calculateFlags(loan: LoanWithAssociationsEntity, status: LoanStatus): LoanFeatureFlags {
        val isActive = status == LoanStatus.ACTIVE
        var isLoanReAged = false
        var isLoanReAmortized = false
        var disbursementCount = 0

        loan.transactions.forEach { lt ->
            if (lt.manuallyReversed == false) {
                if (isActive) {
                    if (lt.type?.reAge == true) {
                        isLoanReAged = true
                    } else if (lt.type?.reAmortize == true) {
                        isLoanReAmortized = true
                    }
                }

                if (lt.type?.disbursement == true) {
                    disbursementCount++
                }
            }
        }

        return LoanFeatureFlags(
            enableBuyDownFee = loan.enableBuyDownFee,
            enableIncomeCapitalization = loan.enableIncomeCapitalization,
            multiDisburseLoan = loan.multiDisburseLoan,
            canDisburse = loan.canDisburse,
            moreThanOneDisbursement = disbursementCount > 1,
            recalculateInterest = loan.isInterestRecalculationEnabled,
            chargedOff = loan.chargedOff,
            loanReAged = isLoanReAged,
            loanReAmortized = isLoanReAmortized,
            isVariableInstallmentsAllowed = loan.allowPartialPeriodInterestCalculation,
            advancedPaymentAllocationStrategy = loan.syncDisbursementWithMeeting,
            canAssignLoanOfficer = loan.loanOfficerName.isBlank(),
        )
    }

    private fun generateActions(status: LoanStatus, flags: LoanFeatureFlags) {
        val generatedActions = mutableListOf<LoanAccountActionItem>()

        when (status) {
            LoanStatus.ACTIVE -> {
                generatedActions.add(LoanAccountActionItem.AddLoanCharge)
                generatedActions.add(LoanAccountActionItem.Foreclosure)
                generatedActions.add(LoanAccountActionItem.MakeRepayment)
                generatedActions.add(LoanAccountActionItem.WaiveInterest)
                generatedActions.add(LoanAccountActionItem.Reschedule)
                generatedActions.add(LoanAccountActionItem.WriteOff)
                generatedActions.add(LoanAccountActionItem.CloseAsRescheduled)
                generatedActions.add(LoanAccountActionItem.Close)
                generatedActions.add(LoanAccountActionItem.LoanScreenReport)
                generatedActions.add(LoanAccountActionItem.CreateGuarantors)
                generatedActions.add(LoanAccountActionItem.ViewGuarantors)
                generatedActions.add(LoanAccountActionItem.RecoverFromGuarantor)
                generatedActions.add(LoanAccountActionItem.SellLoan)
                generatedActions.add(LoanAccountActionItem.ContractTermination)

                if (flags.enableBuyDownFee) {
                    generatedActions.add(LoanAccountActionItem.BuyDownFee)
                }

                if (flags.enableIncomeCapitalization) {
                    generatedActions.add(LoanAccountActionItem.CapitalizedIncome)
                }

                if (flags.multiDisburseLoan || flags.canDisburse) {
                    generatedActions.add(LoanAccountActionItem.Disburse)
                }

                if (flags.multiDisburseLoan && flags.moreThanOneDisbursement) {
                    generatedActions.add(LoanAccountActionItem.UndoDisbursal)
                }

                if (flags.canAssignLoanOfficer) {
                    generatedActions.add(LoanAccountActionItem.AssignLoanOfficer)
                } else {
                    generatedActions.add(LoanAccountActionItem.ChangeLoanOfficer)
                }

                if (flags.recalculateInterest) {
                    generatedActions.add(LoanAccountActionItem.AddInterestPause)

                    generatedActions.add(LoanAccountActionItem.PrepayLoan)
                }

                if (flags.chargedOff) {
                    generatedActions.add(LoanAccountActionItem.UndoChargeOff)
                } else {
                    generatedActions.add(LoanAccountActionItem.ChargeOff)
                }

                if (flags.loanReAged) {
                    generatedActions.add(LoanAccountActionItem.UndoReAge)
                } else {
                    generatedActions.add(LoanAccountActionItem.ReAge)
                }

                if (flags.loanReAmortized) {
                    generatedActions.add(LoanAccountActionItem.UndoReAmortize)
                } else {
                    generatedActions.add(LoanAccountActionItem.ReAmortize)
                }
                generatedActions.add(LoanAccountActionItem.Payments)
            }

            LoanStatus.PENDING -> {
                generatedActions.add(LoanAccountActionItem.AddLoanCharge)
                generatedActions.add(LoanAccountActionItem.Approve)
                generatedActions.add(LoanAccountActionItem.ModifyApplication)
                generatedActions.add(LoanAccountActionItem.Reject)
                generatedActions.add(LoanAccountActionItem.WithdrawnByClient)
                generatedActions.add(LoanAccountActionItem.Delete)
                generatedActions.add(LoanAccountActionItem.AddCollateral)
                generatedActions.add(LoanAccountActionItem.CreateGuarantors)
                generatedActions.add(LoanAccountActionItem.ViewGuarantors)
                generatedActions.add(LoanAccountActionItem.LoanScreenReport)

                if (flags.canAssignLoanOfficer) {
                    generatedActions.add(LoanAccountActionItem.AssignLoanOfficer)
                } else {
                    generatedActions.add(LoanAccountActionItem.ChangeLoanOfficer)
                }

                if (flags.isVariableInstallmentsAllowed) {
                    generatedActions.add(LoanAccountActionItem.EditRepaymentSchedule)
                }
            }

            LoanStatus.APPROVED -> {
                generatedActions.add(LoanAccountActionItem.Disburse)
                generatedActions.add(LoanAccountActionItem.DisburseToSavings)
                generatedActions.add(LoanAccountActionItem.UndoApproval)
                generatedActions.add(LoanAccountActionItem.AddLoanCharge)
                generatedActions.add(LoanAccountActionItem.LoanScreenReport)
            }

            LoanStatus.OVERPAID -> {
                generatedActions.add(LoanAccountActionItem.TransferFunds)
                generatedActions.add(LoanAccountActionItem.CreditBalanceRefund)

                if (flags.multiDisburseLoan) {
                    generatedActions.add(LoanAccountActionItem.Disburse)
                }

                if (flags.advancedPaymentAllocationStrategy) {
                    generatedActions.add(LoanAccountActionItem.Reschedule)
                }
            }

            LoanStatus.CLOSED_WRITTEN_OFF -> {
                generatedActions.add(LoanAccountActionItem.RecoveryPayment)
                generatedActions.add(LoanAccountActionItem.UndoWriteOff)
            }

            LoanStatus.CLOSED_OBLIGATIONS_MET -> {
                generatedActions.add(LoanAccountActionItem.GoodwillCredit)
                generatedActions.add(LoanAccountActionItem.InterestPaymentWaiver)
                generatedActions.add(LoanAccountActionItem.PaymentRefund)
                generatedActions.add(LoanAccountActionItem.MerchantIssuedRefund)

                if (flags.multiDisburseLoan) {
                    generatedActions.add(LoanAccountActionItem.Disburse)
                }

                if (flags.advancedPaymentAllocationStrategy) {
                    generatedActions.add(LoanAccountActionItem.Reschedule)
                }
            }

            LoanStatus.UNKNOWN -> { }
        }

        mutableStateFlow.update {
            it.copy(
                isLoading = false,
                dialogState = null,
                actions = generatedActions,
            )
        }
    }

    override fun handleAction(action: LoanAccountActionsAction) {
        when (action) {
            LoanAccountActionsAction.NavigateBack -> sendEvent(LoanAccountActionsEvent.NavigateBack)
            is LoanAccountActionsAction.OnActionClick -> {
                sendEvent(LoanAccountActionsEvent.NavigateToAction(action.action, route.loanId))
            }
            LoanAccountActionsAction.OnRetry -> {
                if (stateFlow.value.networkConnection) {
                    loadLoanAccountDetails(route.loanId)
                } else {
                    mutableStateFlow.update {
                        it.copy(dialogState = LoanAccountActionsState.DialogState.Error(Res.string.feature_loan_profile_error_network_not_available))
                    }
                }
            }
        }
    }
}

@Serializable
enum class LoanStatus {
    ACTIVE,
    PENDING,
    APPROVED,
    OVERPAID,
    CLOSED_WRITTEN_OFF,
    CLOSED_OBLIGATIONS_MET,
    UNKNOWN,
}

@Serializable
data class LoanFeatureFlags(
    val enableBuyDownFee: Boolean = false,
    val enableIncomeCapitalization: Boolean = false,
    val multiDisburseLoan: Boolean = false,
    val canDisburse: Boolean = false,
    val moreThanOneDisbursement: Boolean = false,
    val recalculateInterest: Boolean = false,
    val chargedOff: Boolean = false,
    val loanReAged: Boolean = false,
    val loanReAmortized: Boolean = false,
    val isVariableInstallmentsAllowed: Boolean = false,
    val advancedPaymentAllocationStrategy: Boolean = false,
    val canAssignLoanOfficer: Boolean = false,
)

fun String?.toLoanStatus(): LoanStatus {
    return when (this) {
        "Submitted and pending approval" -> LoanStatus.PENDING
        "Approved" -> LoanStatus.APPROVED
        "Active" -> LoanStatus.ACTIVE
        "Closed (obligations met)" -> LoanStatus.CLOSED_OBLIGATIONS_MET
        "Closed (written off)" -> LoanStatus.CLOSED_WRITTEN_OFF
        "Overpaid" -> LoanStatus.OVERPAID
        else -> LoanStatus.UNKNOWN
    }
}
data class LoanAccountActionsState(
    val isLoading: Boolean = true,
    val actions: List<LoanAccountActionItem> = emptyList(),
    val dialogState: DialogState? = null,
    val networkConnection: Boolean = false,
) {
    sealed interface DialogState {
        data class Error(val message: StringResource) : DialogState
        data object Loading : DialogState
    }
}

sealed interface LoanAccountActionsEvent {
    data object NavigateBack : LoanAccountActionsEvent
    data class NavigateToAction(val action: LoanAccountActionItem, val loanId: Int) : LoanAccountActionsEvent
}

sealed interface LoanAccountActionsAction {
    data object NavigateBack : LoanAccountActionsAction
    data class OnActionClick(val action: LoanAccountActionItem) : LoanAccountActionsAction
    data object OnRetry : LoanAccountActionsAction
}
