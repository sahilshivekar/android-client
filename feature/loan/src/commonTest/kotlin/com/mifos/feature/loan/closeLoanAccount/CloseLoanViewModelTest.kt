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

import androidx.lifecycle.SavedStateHandle
import com.mifos.core.common.utils.DataState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CloseLoanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCloseLoanRepo: FakeCloseLoanRepository
    private lateinit var fakeLoanRepo: FakeLoanAccountSummaryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCloseLoanRepo = FakeCloseLoanRepository()
        fakeLoanRepo = FakeLoanAccountSummaryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(loanId: Int = 1) = CloseLoanViewModel(
        savedStateHandle = SavedStateHandle(mapOf("loanId" to loanId)),
        repository = fakeCloseLoanRepo,
        loanRepository = fakeLoanRepo,
    )

    @Test
    fun initialState_isTemplateLoading() {
        val viewModel = createViewModel()
        val state = viewModel.stateFlow.value
        assertTrue(state.isTemplateLoading)
        assertNull(state.closedOnDateMillis)
        assertEquals("", state.note)
    }

    @Test
    fun loanId_isReadFromSavedStateHandle() {
        val viewModel = createViewModel(loanId = 42)
        assertEquals(42, viewModel.loanId)
    }

    @Test
    fun loadTemplate_whenTemplateAndLoanSucceed_clearsLoadingAndKeepsNoError() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isTemplateLoading)
        assertNull(state.loadError)
    }

    @Test
    fun loadTemplate_whenTemplateFails_setsLoadError() = runTest {
        fakeCloseLoanRepo.templateToReturn = DataState.Error(Exception("Network error"), null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.stateFlow.value.loadError)
    }

    @Test
    fun loadTemplate_whenLoanFetchFails_setsLoadError() = runTest {
        fakeLoanRepo.loanToReturn = DataState.Error(Exception("Not found"), null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.stateFlow.value.loadError)
    }

    @Test
    fun onSubmit_whenNoDatePicked_isNoOp() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        assertNull(fakeCloseLoanRepo.lastCloseRequest)
        assertEquals(0, fakeCloseLoanRepo.syncCallCount)
    }

    @Test
    fun onSubmit_whenDatePicked_sendsRequestAndClearsDialog() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnNoteChange("paid off"))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        assertNotNull(fakeCloseLoanRepo.lastCloseRequest)
        assertEquals(1, fakeCloseLoanRepo.syncCallCount)
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun onSubmit_whenCloseThrows_emitsErrorDialogAndSkipsSync() = runTest {
        fakeCloseLoanRepo.closeShouldThrow = RuntimeException("Server error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.dialogState is CloseLoanState.DialogState.Error)
        assertEquals(0, fakeCloseLoanRepo.syncCallCount)
    }

    @Test
    fun onSubmit_whenSyncThrows_stillSucceedsBecauseCloseIsAuthoritative() = runTest {
        fakeCloseLoanRepo.syncShouldThrow = RuntimeException("Sync failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.dialogState)
        assertEquals(1, fakeCloseLoanRepo.syncCallCount)
    }

    @Test
    fun onSubmit_withBlankNote_excludesNoteFromRequest() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnNoteChange("   "))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertFalse(request.containsKey("note"))
    }

    @Test
    fun onSubmit_withNonBlankNote_includesNoteInRequest() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnNoteChange("early repayment"))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertEquals("early repayment", request["note"])
    }

    @Test
    fun onSubmit_requestContainsRequiredFields() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("15 March 2025")))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertEquals("15 March 2025", request["closedOnDate"])
        assertEquals("dd MMMM yyyy", request["dateFormat"])
        assertEquals("en", request["locale"])
    }

    @Test
    fun showAndHideDatePicker_togglesStateFlag() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnShowDatePicker)
        advanceUntilIdle()
        assertTrue(viewModel.stateFlow.value.showDatePicker)

        viewModel.trySendAction(CloseLoanAction.OnHideDatePicker)
        advanceUntilIdle()
        assertFalse(viewModel.stateFlow.value.showDatePicker)
    }

    @Test
    fun onDismissError_clearsDialogState() = runTest {
        fakeCloseLoanRepo.closeShouldThrow = RuntimeException("boom")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trySendAction(CloseLoanAction.OnDateChange(dateMillis("01 January 2025")))
        viewModel.trySendAction(CloseLoanAction.OnSubmit)
        advanceUntilIdle()
        assertTrue(viewModel.stateFlow.value.dialogState is CloseLoanState.DialogState.Error)

        viewModel.trySendAction(CloseLoanAction.OnDismissError)
        advanceUntilIdle()
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    /**
     * Builds a millis value that, when round-tripped through `ApiDateFormatter.formatForApi`,
     * produces the given `dd MMMM yyyy` string. Uses the system default timezone so that it
     * matches the formatter's conversion on the same machine.
     */
    private fun dateMillis(ddMMMMyyyy: String): Long {
        val parts = ddMMMMyyyy.split(" ")
        val day = parts[0].toInt()
        val month = when (parts[1]) {
            "January" -> 1
            "February" -> 2
            "March" -> 3
            "April" -> 4
            "May" -> 5
            "June" -> 6
            "July" -> 7
            "August" -> 8
            "September" -> 9
            "October" -> 10
            "November" -> 11
            "December" -> 12
            else -> error("bad month ${parts[1]}")
        }
        val year = parts[2].toInt()
        return kotlinx.datetime.LocalDate(year, month, day)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }
}
