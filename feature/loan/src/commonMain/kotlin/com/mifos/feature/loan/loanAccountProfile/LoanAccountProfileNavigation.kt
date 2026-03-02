/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.loanAccountProfile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mifos.core.model.objects.account.loan.LoanWithAssociations
import kotlinx.serialization.Serializable

@Serializable
data class LoanAccountRoute(
    val loanId: Int = -1,
)

fun NavGraphBuilder.loanProfileAccountDestination(
    onNavigateBack: () -> Unit,
    navController: NavController,
    navigateToRepaymentSchedule: (Int) -> Unit,
    navigateToLoanDetails: (Int) -> Unit,
    navigateToTransactions: (Int) -> Unit,
    navigateToCharges: (Int) -> Unit,
    navigateToDocuments: (Int) -> Unit,
    navigateToReschedules: (Int) -> Unit,
    navigateToNotes: (Int) -> Unit,
    approveLoan: (Int, LoanWithAssociations) -> Unit,
    onRepaymentClick: (LoanWithAssociations) -> Unit,
    navigateToTransferScreen: (loanId: Int, accountNumber: String, clientId: Int, currencyCode: String, officeId: Int) -> Unit,
) {
    composable<LoanAccountRoute> {
        LoanAccountProfileScreen(
            onNavigateBack = onNavigateBack,
            navController = navController,
            navigateToRepaymentSchedule = navigateToRepaymentSchedule,
            navigateToLoanDetails = navigateToLoanDetails,
            navigateToTransactions = navigateToTransactions,
            navigateToCharges = navigateToCharges,
            navigateToDocuments = navigateToDocuments,
            navigateToReschedules = navigateToReschedules,
            navigateToNotes = navigateToNotes,
            approveLoan = approveLoan,
            onRepaymentClick = onRepaymentClick,
            navigateToTransferScreen = navigateToTransferScreen,
        )
    }
}

fun NavController.navigateToLoanAccountProfileScreen(loanId: Int) {
    this.navigate(
        LoanAccountRoute(
            loanId = loanId,
        ),
    )
}
