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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun initialState_isLoading() {
        val viewModel = createViewModel()
        assertEquals(CloseLoanUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loanId_isReadFromSavedStateHandle() {
        val viewModel = createViewModel(loanId = 42)
        assertEquals(42, viewModel.loanId)
    }

    @Test
    fun loadTemplate_whenTemplateAndLoanSucceed_emitsTemplateLoaded() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CloseLoanUiState.TemplateLoaded)
    }

    @Test
    fun loadTemplate_whenTemplateFails_emitsError() = runTest {
        fakeCloseLoanRepo.templateToReturn = DataState.Error(Exception("Network error"), null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CloseLoanUiState.Error)
    }

    @Test
    fun loadTemplate_whenLoanFetchFails_emitsError() = runTest {
        fakeLoanRepo.loanToReturn = DataState.Error(Exception("Not found"), null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CloseLoanUiState.Error)
    }

    @Test
    fun closeLoan_whenSuccessful_emitsClosedSuccessfully() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "paid off")
        advanceUntilIdle()

        assertEquals(CloseLoanUiState.ClosedSuccessfully, viewModel.uiState.value)
    }

    @Test
    fun closeLoan_whenCloseThrows_emitsError() = runTest {
        fakeCloseLoanRepo.closeShouldThrow = RuntimeException("Server error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CloseLoanUiState.Error)
    }

    @Test
    fun closeLoan_whenSyncThrows_emitsError() = runTest {
        fakeCloseLoanRepo.syncShouldThrow = RuntimeException("Sync failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CloseLoanUiState.Error)
    }

    @Test
    fun closeLoan_callsSyncAfterClose() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "")
        advanceUntilIdle()

        assertEquals(1, fakeCloseLoanRepo.syncCallCount)
    }

    @Test
    fun closeLoan_withNonBlankNote_includesNoteInRequest() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "early repayment")
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertEquals("early repayment", request["note"])
    }

    @Test
    fun closeLoan_withBlankNote_excludesNoteFromRequest() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "01 January 2025", note = "   ")
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertFalse(request.containsKey("note"))
    }

    @Test
    fun closeLoan_requestContainsRequiredFields() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.closeLoan(closedOnDate = "15 March 2025", note = "")
        advanceUntilIdle()

        val request = fakeCloseLoanRepo.lastCloseRequest
        assertNotNull(request)
        assertEquals("15 March 2025", request["closedOnDate"])
        assertEquals("dd MMMM yyyy", request["dateFormat"])
        assertEquals("en", request["locale"])
    }
}
