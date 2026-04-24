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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mifos.core.common.utils.DataState
import com.mifos.core.data.repository.CloseLoanRepository
import com.mifos.core.data.repository.LoanAccountSummaryRepository
import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import com.mifos.room.entities.templates.loans.LoanTransactionTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloseLoanViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CloseLoanRepository,
    private val loanRepository: LoanAccountSummaryRepository,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<CloseLoanScreenRoute>()
    val loanId: Int get() = args.loanId

    private val _uiState = MutableStateFlow<CloseLoanUiState>(CloseLoanUiState.Loading)
    val uiState: StateFlow<CloseLoanUiState> = _uiState.asStateFlow()

    init {
        loadTemplate()
    }

    private fun loadTemplate() {
        viewModelScope.launch {
            _uiState.value = CloseLoanUiState.Loading
            try {
                repository.getCloseLoanTemplate(loanId).collect { templateState ->
                    if (templateState is DataState.Success) {
                        loanRepository.getLoanById(loanId).collect { loanState ->
                            if (loanState is DataState.Success) {
                                _uiState.value = CloseLoanUiState.TemplateLoaded(
                                    template = templateState.data ?: LoanTransactionTemplate(),
                                    loan = loanState.data
                                )
                            } else if (loanState is DataState.Error) {
                                _uiState.value = CloseLoanUiState.Error(Res.string.feature_loan_profile_failed_to_load_loan)
                            }
                        }
                    } else if (templateState is DataState.Error) {
                        _uiState.value = CloseLoanUiState.Error(Res.string.feature_loan_close_failed_to_load_template)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CloseLoanUiState.Error(Res.string.feature_loan_close_failed_to_load_template)
            }
        }
    }

    fun closeLoan(closedOnDate: String, note: String) {
        viewModelScope.launch {
            _uiState.value = CloseLoanUiState.Loading
            try {
                val request = buildMap<String, String> {
                    put("closedOnDate", closedOnDate)
                    put("dateFormat", "dd MMMM yyyy")
                    put("locale", "en")
                    if (note.isNotBlank()) put("note", note)
                }
                repository.closeLoanAccount(loanId, request)
                repository.syncLoanAccount(loanId)
                _uiState.value = CloseLoanUiState.ClosedSuccessfully
            } catch (e: Exception) {
                _uiState.value = CloseLoanUiState.Error(Res.string.feature_loan_close_failed)
            }
        }
    }
}
