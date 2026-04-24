/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.core.data.repository

import com.mifos.core.common.utils.DataState
import com.mifos.room.entities.templates.loans.LoanTransactionTemplate
import kotlinx.coroutines.flow.Flow

/** Repository abstraction for the close-loan transaction flow. */
interface CloseLoanRepository {

    /** Fetches the close-loan template (`command=close`) for [loanId]. */
    fun getCloseLoanTemplate(loanId: Int): Flow<DataState<LoanTransactionTemplate?>>

    /**
     * Submits a close transaction for [loanId] with the given [request] payload.
     *
     * Expected keys: `closedOnDate`, `dateFormat`, `locale`, and optionally `note`.
     */
    suspend fun closeLoanAccount(loanId: Int, request: Map<String, String>)

    /** Re-fetches [loanId] so local state reflects the post-close status. */
    suspend fun syncLoanAccount(loanId: Int)
}
