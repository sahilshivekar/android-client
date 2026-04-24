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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Navigation route for the Close Loan Account screen.
 *
 * @property loanId id of the loan account being closed.
 */
@Serializable
data class CloseLoanScreenRoute(
    val loanId: Int,
)

/**
 * Key written into the previous back-stack entry's `SavedStateHandle` when a close succeeds.
 *
 * The Loan Account Profile screen observes this key to know it must reload the loan so the
 * status reflects the new "Closed" state.
 */
const val LOAN_CLOSED_RESULT_KEY: String = "close_loan_account_result_closed"

/**
 * Registers the Close Loan Account destination.
 *
 * On successful close we write [LOAN_CLOSED_RESULT_KEY] into the previous back-stack entry's
 * `SavedStateHandle` so the caller can refresh its state, then pop.
 */
fun NavGraphBuilder.closeLoanAccountScreen(
    navController: NavController,
    onBackPressed: () -> Unit,
) {
    composable<CloseLoanScreenRoute> {
        CloseLoanScreen(
            onBackPressed = onBackPressed,
            onCloseSuccess = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(LOAN_CLOSED_RESULT_KEY, true)
                navController.popBackStack()
            },
        )
    }
}

/** Navigates to the Close Loan Account screen for [loanId]. */
fun NavController.navigateToCloseLoanScreen(loanId: Int) {
    navigate(CloseLoanScreenRoute(loanId = loanId))
}
