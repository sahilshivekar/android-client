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

import org.jetbrains.compose.resources.StringResource

/**
 * UI state for the Close Loan Account screen.
 *
 * The `Closed On` date starts as `null` so the user must explicitly pick one before submission
 * is enabled. `disbursementDateMillis` is the lower bound enforced by the date picker.
 */
data class CloseLoanState(
    val isTemplateLoading: Boolean = true,
    val loadError: StringResource? = null,
    val closedOnDateMillis: Long? = null,
    val disbursementDateMillis: Long? = null,
    val note: String = "",
    val showDatePicker: Boolean = false,
    val dialogState: DialogState? = null,
) {
    val isSubmitEnabled: Boolean
        get() = closedOnDateMillis != null && dialogState !is DialogState.Submitting

    /** Modal dialog state overlaid on top of the form. */
    sealed interface DialogState {
        /** Shown while the close request is in flight. */
        data object Submitting : DialogState

        /** Shown when the close request failed; dismissable so the user can retry. */
        data class Error(val message: StringResource) : DialogState
    }
}

/** One-shot events emitted by [CloseLoanViewModel] to the screen. */
sealed interface CloseLoanEvent {
    /** Loan was closed successfully — screen should show success feedback and pop. */
    data object CloseSuccess : CloseLoanEvent
}

/** User-originated actions handled by [CloseLoanViewModel]. */
sealed interface CloseLoanAction {
    /** User picked a new `Closed On` date in the date picker. */
    data class OnDateChange(val millis: Long) : CloseLoanAction

    /** User typed into the note field. */
    data class OnNoteChange(val note: String) : CloseLoanAction

    /** User tapped the `Closed On` field to open the date picker. */
    data object OnShowDatePicker : CloseLoanAction

    /** User dismissed the date picker without confirming. */
    data object OnHideDatePicker : CloseLoanAction

    /** User tapped the Submit button. */
    data object OnSubmit : CloseLoanAction

    /** User dismissed the error dialog. */
    data object OnDismissError : CloseLoanAction

    /** User tapped retry after the initial template/loan load failed. */
    data object OnRetryLoadTemplate : CloseLoanAction
}
