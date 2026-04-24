/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.closeLoanAccount

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_close_failed
import androidclient.feature.loan.generated.resources.feature_loan_close_failed_to_load_template
import androidclient.feature.loan.generated.resources.feature_loan_profile_failed_to_load_loan
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mifos.core.common.utils.ApiDateFormatter
import com.mifos.core.common.utils.DataState
import com.mifos.core.common.utils.DateHelper
import com.mifos.core.data.repository.CloseLoanRepository
import com.mifos.core.data.repository.LoanAccountSummaryRepository
import com.mifos.core.ui.util.BaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Close Loan Account screen.
 *
 * Loads the close-loan template and the current loan (to get the disbursement date used as the
 * picker lower bound), then handles the submit flow via [CloseLoanAction.OnSubmit].
 */
class CloseLoanViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CloseLoanRepository,
    private val loanRepository: LoanAccountSummaryRepository,
) : BaseViewModel<CloseLoanState, CloseLoanEvent, CloseLoanAction>(
    initialState = CloseLoanState(),
) {

    private val route = savedStateHandle.toRoute<CloseLoanScreenRoute>()

    /** The loan id this screen is operating on. */
    val loanId: Int get() = route.loanId

    init {
        loadTemplate()
    }

    override fun handleAction(action: CloseLoanAction) {
        when (action) {
            is CloseLoanAction.OnDateChange -> mutableStateFlow.update {
                it.copy(closedOnDateMillis = action.millis, showDatePicker = false)
            }
            is CloseLoanAction.OnNoteChange -> mutableStateFlow.update {
                it.copy(note = action.note)
            }
            CloseLoanAction.OnShowDatePicker -> mutableStateFlow.update {
                it.copy(showDatePicker = true)
            }
            CloseLoanAction.OnHideDatePicker -> mutableStateFlow.update {
                it.copy(showDatePicker = false)
            }
            CloseLoanAction.OnSubmit -> submitClose()
            CloseLoanAction.OnDismissError -> mutableStateFlow.update {
                it.copy(dialogState = null)
            }
            CloseLoanAction.OnRetryLoadTemplate -> loadTemplate()
        }
    }

    private fun loadTemplate() {
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(isTemplateLoading = true, loadError = null) }
            val templateResult = repository.getCloseLoanTemplate(loanId)
                .first { it !is DataState.Loading }
            if (templateResult is DataState.Error) {
                mutableStateFlow.update {
                    it.copy(
                        isTemplateLoading = false,
                        loadError = Res.string.feature_loan_close_failed_to_load_template,
                    )
                }
                return@launch
            }

            val loanResult = loanRepository.getLoanById(loanId)
                .first { it !is DataState.Loading }
            when (loanResult) {
                is DataState.Success -> {
                    val disbursement = loanResult.data
                        ?.timeline
                        ?.actualDisbursementDate
                        ?.filterNotNull()
                        ?.let { DateHelper.getDateAsLongFromList(it) }
                    mutableStateFlow.update {
                        it.copy(
                            isTemplateLoading = false,
                            disbursementDateMillis = disbursement,
                        )
                    }
                }
                is DataState.Error -> mutableStateFlow.update {
                    it.copy(
                        isTemplateLoading = false,
                        loadError = Res.string.feature_loan_profile_failed_to_load_loan,
                    )
                }
                DataState.Loading -> Unit
            }
        }
    }

    private fun submitClose() {
        val closedOnMillis = state.closedOnDateMillis ?: return
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(dialogState = CloseLoanState.DialogState.Submitting) }

            val closedOnDate = ApiDateFormatter.formatForApi(closedOnMillis)
            val noteValue = state.note
            val request = buildMap<String, String> {
                put("closedOnDate", closedOnDate)
                put("dateFormat", ApiDateFormatter.DATE_FORMAT)
                put("locale", ApiDateFormatter.LOCALE)
                if (noteValue.isNotBlank()) put("note", noteValue)
            }

            try {
                repository.closeLoanAccount(loanId, request)
            } catch (e: Exception) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = CloseLoanState.DialogState.Error(
                            Res.string.feature_loan_close_failed,
                        ),
                    )
                }
                return@launch
            }

            // Close succeeded — surface success to the user immediately, then sync best-effort.
            mutableStateFlow.update { it.copy(dialogState = null) }
            sendEvent(CloseLoanEvent.CloseSuccess)
            runCatching { repository.syncLoanAccount(loanId) }
        }
    }
}
