/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.mifos.feature.loan.closeLoanAccount

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_close_cancel
import androidclient.feature.loan.generated.resources.feature_loan_close_closed_on
import androidclient.feature.loan.generated.resources.feature_loan_close_date_required
import androidclient.feature.loan.generated.resources.feature_loan_close_loan_account
import androidclient.feature.loan.generated.resources.feature_loan_close_note
import androidclient.feature.loan.generated.resources.feature_loan_close_submit
import androidclient.feature.loan.generated.resources.feature_loan_close_success
import androidclient.feature.loan.generated.resources.feature_loan_disbursed_date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mifos.core.common.utils.DateHelper
import com.mifos.core.designsystem.component.MifosButton
import com.mifos.core.designsystem.component.MifosDatePickerTextField
import com.mifos.core.designsystem.component.MifosOutlinedTextField
import com.mifos.core.designsystem.component.MifosScaffold
import com.mifos.core.designsystem.theme.DesignToken
import com.mifos.core.ui.components.MifosErrorComponent
import com.mifos.core.ui.components.MifosProgressIndicator
import com.mifos.room.entities.accounts.loans.LoanWithAssociationsEntity
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.designsystem.theme.KptTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
internal fun CloseLoanScreen(
    onBackPressed: () -> Unit,
    viewModel: CloseLoanViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(Res.string.feature_loan_close_success)

    LaunchedEffect(uiState) {
        if (uiState is CloseLoanUiState.ClosedSuccessfully) {
            scope.launch {
                snackbarHostState.showSnackbar(successMessage)
                onBackPressed()
            }
        }
    }

    MifosScaffold(
        title = stringResource(Res.string.feature_loan_close_loan_account),
        onBackPressed = onBackPressed,
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is CloseLoanUiState.Loading -> MifosProgressIndicator()

                is CloseLoanUiState.Error -> {
                    MifosErrorComponent(
                        isNetworkConnected = true,
                        message = stringResource(state.message),
                        isRetryEnabled = false,
                        onRetry = {},
                    )
                }

                is CloseLoanUiState.TemplateLoaded -> {
                    CloseLoanContent(
                        initialDateMillis = DateHelper.getDateAsLongFromList(state.template.date),
                        loan = state.loan,
                        onSubmit = { closedOnDate, note ->
                            viewModel.closeLoan(closedOnDate, note)
                        },
                        onCancel = onBackPressed,
                    )
                }

                is CloseLoanUiState.ClosedSuccessfully -> {
                    // Handled in LaunchedEffect
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun CloseLoanContent(
    initialDateMillis: Long?,
    loan: LoanWithAssociationsEntity?,
    onSubmit: (closedOnDate: String, note: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var closedOnDateMillis by rememberSaveable {
        mutableStateOf(initialDateMillis ?: Clock.System.now().toEpochMilliseconds())
    }
    val disbursementDateMillis = loan?.timeline?.actualDisbursementDate?.filterNotNull()?.let {
        DateHelper.getDateAsLongFromList(it)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = closedOnDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return disbursementDateMillis == null || utcTimeMillis >= disbursementDateMillis
            }
        },
    )
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var dateErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val dateRequiredError = stringResource(Res.string.feature_loan_close_date_required)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            closedOnDateMillis = it
                            dateErrorMessage = null
                        }
                    },
                ) { Text(stringResource(Res.string.feature_loan_close_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.feature_loan_close_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Spacer(modifier = Modifier.height(KptTheme.spacing.sm))

        MifosDatePickerTextField(
            value = DateHelper.getDateAsStringFromLong(closedOnDateMillis),
            label = stringResource(Res.string.feature_loan_close_closed_on),
            modifier = Modifier.fillMaxWidth(),
            openDatePicker = { showDatePicker = true },
            errorMessage = dateErrorMessage,
        )

        loan?.timeline?.actualDisbursementDate?.filterNotNull()?.let {
            Text(
                text = stringResource(Res.string.feature_loan_disbursed_date) + ": " +
                    DateHelper.getDateAsString(it),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = KptTheme.spacing.md),
            )
        }

        MifosOutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.feature_loan_close_note),
            error = null,
        )

        Spacer(modifier = Modifier.height(KptTheme.spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            MifosButton(
                modifier = Modifier.weight(1f),
                onClick = onCancel,
            ) {
                Text(text = stringResource(Res.string.feature_loan_close_cancel))
            }

            MifosButton(
                modifier = Modifier.weight(1f),
                enabled = DateHelper.getDateAsStringFromLong(closedOnDateMillis).isNotBlank(),
                onClick = {
                    val formattedDate = DateHelper.getDateAsStringFromLong(closedOnDateMillis)
                    onSubmit(formattedDate, note)
                },
            ) {
                Text(text = stringResource(Res.string.feature_loan_close_submit))
            }
        }
    }
}
