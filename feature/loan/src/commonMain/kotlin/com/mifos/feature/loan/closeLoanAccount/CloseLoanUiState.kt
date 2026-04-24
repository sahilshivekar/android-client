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

import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import com.mifos.room.entities.templates.loans.LoanTransactionTemplate
import org.jetbrains.compose.resources.StringResource

sealed class CloseLoanUiState {

    data object Loading : CloseLoanUiState()

    data class Error(val message: StringResource) : CloseLoanUiState()

    data class TemplateLoaded(
        val template: LoanTransactionTemplate,
        val loan: LoanWithAssociationsEntity?,
    ) : CloseLoanUiState()

    data object ClosedSuccessfully : CloseLoanUiState()
}


