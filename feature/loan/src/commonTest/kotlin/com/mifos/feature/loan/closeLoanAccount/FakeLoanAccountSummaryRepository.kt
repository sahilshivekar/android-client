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

import com.mifos.core.common.utils.DataState
import com.mifos.core.data.repository.LoanAccountSummaryRepository
import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeLoanAccountSummaryRepository : LoanAccountSummaryRepository {

    var loanToReturn: DataState<LoanWithAssociationsEntity?> =
        DataState.Success(LoanWithAssociationsEntity())

    override fun getLoanById(loanId: Int): Flow<DataState<LoanWithAssociationsEntity?>> =
        flowOf(loanToReturn)
}
