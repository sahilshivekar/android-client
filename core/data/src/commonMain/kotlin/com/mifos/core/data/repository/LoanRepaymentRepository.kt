/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.core.data.repository

import com.mifos.room.entities.accounts.loans.LoanRepaymentRequestEntity
import com.mifos.room.entities.accounts.loans.LoanRepaymentResponseEntity
import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import com.mifos.room.entities.templates.loans.LoanRepaymentTemplateEntity

interface LoanRepaymentRepository {

    suspend fun getLoanRepayTemplate(loanId: Int): LoanRepaymentTemplateEntity?

    suspend fun submitPayment(
        loanId: Int,
        request: LoanRepaymentRequestEntity,
    ): LoanRepaymentResponseEntity

    suspend fun getDatabaseLoanRepaymentByLoanId(loanId: Int): LoanRepaymentRequestEntity?

    suspend fun getLoanById(loanId: Int): LoanWithAssociationsEntity?
}
