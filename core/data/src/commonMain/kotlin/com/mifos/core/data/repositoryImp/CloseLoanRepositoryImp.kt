/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.core.data.repositoryImp

import com.mifos.core.common.utils.DataState
import com.mifos.core.common.utils.asDataStateFlow
import com.mifos.core.data.repository.CloseLoanRepository
import com.mifos.core.network.datamanager.DataManagerLoan
import com.mifos.room.entities.templates.loans.LoanTransactionTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Default [CloseLoanRepository] backed by [DataManagerLoan]. */
class CloseLoanRepositoryImp(
    private val dataManagerLoan: DataManagerLoan,
) : CloseLoanRepository {

    override fun getCloseLoanTemplate(loanId: Int): Flow<DataState<LoanTransactionTemplate?>> {
        return dataManagerLoan.getLoanTransactionTemplate(loanId, "close")
            .asDataStateFlow()
    }

    override suspend fun closeLoanAccount(loanId: Int, request: Map<String, String>) {
        dataManagerLoan.closeLoanAccount(loanId, request)
    }

    override suspend fun syncLoanAccount(loanId: Int) {
        dataManagerLoan.syncLoanById(loanId).first()
    }
}
