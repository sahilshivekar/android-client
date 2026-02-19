/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-field-officer-app/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.loanRepayment

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_account_number
import androidclient.feature.loan.generated.resources.feature_loan_additional_payment
import androidclient.feature.loan.generated.resources.feature_loan_amount
import androidclient.feature.loan.generated.resources.feature_loan_bank_number
import androidclient.feature.loan.generated.resources.feature_loan_cancel
import androidclient.feature.loan.generated.resources.feature_loan_cheque_number
import androidclient.feature.loan.generated.resources.feature_loan_dialog_action_ok
import androidclient.feature.loan.generated.resources.feature_loan_dialog_action_pay_now
import androidclient.feature.loan.generated.resources.feature_loan_dialog_message_sync_transaction
import androidclient.feature.loan.generated.resources.feature_loan_external_id_field
import androidclient.feature.loan.generated.resources.feature_loan_failed_to_load_loan_repayment
import androidclient.feature.loan.generated.resources.feature_loan_fees
import androidclient.feature.loan.generated.resources.feature_loan_interest
import androidclient.feature.loan.generated.resources.feature_loan_loan_amount_due
import androidclient.feature.loan.generated.resources.feature_loan_loan_fees
import androidclient.feature.loan.generated.resources.feature_loan_loan_in_arrears
import androidclient.feature.loan.generated.resources.feature_loan_loan_repayment
import androidclient.feature.loan.generated.resources.feature_loan_no_penalties_found
import androidclient.feature.loan.generated.resources.feature_loan_note
import androidclient.feature.loan.generated.resources.feature_loan_payment_success_message
import androidclient.feature.loan.generated.resources.feature_loan_payment_type
import androidclient.feature.loan.generated.resources.feature_loan_penalties
import androidclient.feature.loan.generated.resources.feature_loan_principal
import androidclient.feature.loan.generated.resources.feature_loan_receipt_number
import androidclient.feature.loan.generated.resources.feature_loan_repayment_date
import androidclient.feature.loan.generated.resources.feature_loan_review_payment
import androidclient.feature.loan.generated.resources.feature_loan_routing_code
import androidclient.feature.loan.generated.resources.feature_loan_select_date
import androidclient.feature.loan.generated.resources.feature_loan_show_payment_details
import androidclient.feature.loan.generated.resources.feature_loan_sync_previous_transaction
import androidclient.feature.loan.generated.resources.feature_loan_total
import androidclient.feature.loan.generated.resources.feature_loan_transaction_breakdown
import androidclient.feature.loan.generated.resources.feature_loan_waive_penalties
import androidclient.feature.loan.generated.resources.yes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mifos.core.common.utils.CurrencyFormatter
import com.mifos.core.common.utils.DateHelper
import com.mifos.core.designsystem.component.MifosDatePickerTextField
import com.mifos.core.designsystem.component.MifosOutlinedTextField
import com.mifos.core.designsystem.component.MifosScaffold
import com.mifos.core.designsystem.component.MifosSweetError
import com.mifos.core.designsystem.component.MifosTextFieldDropdown
import com.mifos.core.ui.components.MifosCheckBox
import com.mifos.core.ui.components.MifosProgressIndicator
import com.mifos.room.entities.PaymentTypeOptionEntity
import com.mifos.room.entities.accounts.loans.LoanRepaymentRequestEntity
import com.mifos.room.entities.templates.loans.LoanRepaymentTemplateEntity
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.designsystem.theme.KptTheme
import kotlin.time.Clock

@Composable
internal fun LoanRepaymentScreen(
    navigateBack: () -> Unit,
    viewmodel: LoanRepaymentViewModel = koinViewModel(),
) {
    val uiState by viewmodel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle events
    LaunchedEffect(Unit) {
        viewmodel.eventFlow.collect { event ->
            when (event) {
                is LoanRepaymentEvent.PaymentSubmittedSuccessfully -> {
                    if (event.response != null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(Res.string.feature_loan_payment_success_message) + event.response.resourceId,
                            )
                            navigateBack()
                        }
                    }
                }
            }
        }
    }

    LoanRepaymentScreen(
        loanId = loanDetails.loanId,
        clientName = loanDetails.clientName,
        loanProductName = loanDetails.loanProductName,
        amountInArrears = loanDetails.amountInArrears,
        loanAccountNumber = loanDetails.loanAccountNumber,
        uiState = uiState,
        navigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        onRetry = { viewmodel.trySendAction(LoanRepaymentAction.CheckDatabaseLoanRepayment) },
        submitPayment = { viewmodel.trySendAction(LoanRepaymentAction.SubmitPayment(it)) },
        onLoanRepaymentDoesNotExistInDatabase = {
            viewmodel.trySendAction(LoanRepaymentAction.LoadLoanRepaymentTemplate)
        },
        onUpdatePaymentType = viewmodel::updatePaymentType,
        onUpdatePaymentTypeWithId = viewmodel::updatePaymentTypeWithId,
        onUpdateAmount = viewmodel::updateAmount,
        onUpdateAdditionalPayment = viewmodel::updateAdditionalPayment,
        onUpdateFees = viewmodel::updateFees,
        onUpdateRepaymentDate = viewmodel::updateRepaymentDate,
        onUpdateShowPaymentDetails = viewmodel::updateShowPaymentDetails,
        onUpdateAccountNumber = viewmodel::updateAccountNumber,
        onUpdateExternalId = viewmodel::updateExternalId,
        onUpdateChequeNumber = viewmodel::updateChequeNumber,
        onUpdateRoutingCode = viewmodel::updateRoutingCode,
        onUpdateReceiptNumber = viewmodel::updateReceiptNumber,
        onUpdateBankNumber = viewmodel::updateBankNumber,
        onUpdateNote = viewmodel::updateNote,
        onUpdateWaivePenalties = viewmodel::updateWaivePenalties,
    )
}

@Composable
internal fun LoanRepaymentScreen(
    loanId: Int,
    clientName: String,
    loanProductName: String,
    amountInArrears: Double?,
    loanAccountNumber: String,
    uiState: LoanRepaymentUiState,
    navigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    submitPayment: (request: LoanRepaymentRequestEntity) -> Unit,
    onLoanRepaymentDoesNotExistInDatabase: () -> Unit,
    onUpdatePaymentType: (String) -> Unit,
    onUpdatePaymentTypeWithId: (String, Int) -> Unit,
    onUpdateAmount: (String) -> Unit,
    onUpdateAdditionalPayment: (String) -> Unit,
    onUpdateFees: (String) -> Unit,
    onUpdateRepaymentDate: (Long) -> Unit,
    onUpdateShowPaymentDetails: (Boolean) -> Unit,
    onUpdateAccountNumber: (String) -> Unit,
    onUpdateExternalId: (String) -> Unit,
    onUpdateChequeNumber: (String) -> Unit,
    onUpdateRoutingCode: (String) -> Unit,
    onUpdateReceiptNumber: (String) -> Unit,
    onUpdateBankNumber: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateWaivePenalties: (Boolean) -> Unit,
) {
    MifosScaffold(
        snackbarHostState = snackbarHostState,
        onBackPressed = navigateBack,
        title = stringResource(Res.string.feature_loan_loan_repayment),
    ) {
        Box(
            modifier = Modifier.padding(it),
        ) {
            when {
                uiState.isLoading -> {
                    MifosProgressIndicator()
                }

                uiState.error != null -> {
                    MifosSweetError(message = stringResource(uiState.error)) {
                        onRetry()
                    }
                }

                uiState.loanRepaymentTemplate != null -> {
                    LoanRepaymentContent(
                        loanId = loanId,
                        loanAccountNumber = loanAccountNumber,
                        clientName = clientName,
                        loanProductName = loanProductName,
                        amountInArrears = amountInArrears,
                        loanRepaymentTemplate = uiState.loanRepaymentTemplate,
                        uiState = uiState,
                        navigateBack = navigateBack,
                        submitPayment = submitPayment,
                        onUpdatePaymentType = onUpdatePaymentType,
                        onUpdatePaymentTypeWithId = onUpdatePaymentTypeWithId,
                        onUpdateAmount = onUpdateAmount,
                        onUpdateAdditionalPayment = onUpdateAdditionalPayment,
                        onUpdateFees = onUpdateFees,
                        onUpdateRepaymentDate = onUpdateRepaymentDate,
                        onUpdateShowPaymentDetails = onUpdateShowPaymentDetails,
                        onUpdateAccountNumber = onUpdateAccountNumber,
                        onUpdateExternalId = onUpdateExternalId,
                        onUpdateChequeNumber = onUpdateChequeNumber,
                        onUpdateRoutingCode = onUpdateRoutingCode,
                        onUpdateReceiptNumber = onUpdateReceiptNumber,
                        onUpdateBankNumber = onUpdateBankNumber,
                        onUpdateNote = onUpdateNote,
                        onUpdateWaivePenalties = onUpdateWaivePenalties,
                    )
                }

                uiState.hasCheckedDatabase && !uiState.loanRepaymentExistsInDatabase -> {
                    onLoanRepaymentDoesNotExistInDatabase()
                }

                uiState.loanRepaymentExistsInDatabase -> {
                    AlertDialog(
                        onDismissRequest = { },
                        confirmButton = {
                            TextButton(onClick = { navigateBack() }) {
                                Text(text = stringResource(Res.string.feature_loan_dialog_action_ok))
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(Res.string.feature_loan_sync_previous_transaction),
                                style = KptTheme.typography.titleLarge,
                            )
                        },
                        text = { Text(text = stringResource(Res.string.feature_loan_dialog_message_sync_transaction)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanRepaymentContent(
    loanId: Int,
    clientName: String,
    loanProductName: String,
    amountInArrears: Double?,
    loanAccountNumber: String,
    loanRepaymentTemplate: LoanRepaymentTemplateEntity,
    uiState: LoanRepaymentUiState,
    navigateBack: () -> Unit,
    submitPayment: (request: LoanRepaymentRequestEntity) -> Unit,
    onUpdatePaymentType: (String) -> Unit,
    onUpdatePaymentTypeWithId: (String, Int) -> Unit,
    onUpdateAmount: (String) -> Unit,
    onUpdateAdditionalPayment: (String) -> Unit,
    onUpdateFees: (String) -> Unit,
    onUpdateRepaymentDate: (Long) -> Unit,
    onUpdateShowPaymentDetails: (Boolean) -> Unit,
    onUpdateAccountNumber: (String) -> Unit,
    onUpdateExternalId: (String) -> Unit,
    onUpdateChequeNumber: (String) -> Unit,
    onUpdateRoutingCode: (String) -> Unit,
    onUpdateReceiptNumber: (String) -> Unit,
    onUpdateBankNumber: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateWaivePenalties: (Boolean) -> Unit,
) {
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.repaymentDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= Clock.System.now().toEpochMilliseconds()
            }
        },
    )
    val scrollState = rememberScrollState()
    var showConfirmationDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showConfirmationDialog) {
        ShowLoanRepaymentConfirmationDialog(
            onDismiss = { showConfirmationDialog = false },
            loanAccountNumber = loanAccountNumber,
            paymentTypeId = uiState.paymentTypeId.toString(),
            repaymentDate = uiState.repaymentDate,
            paymentType = uiState.paymentType,
            amount = uiState.amount,
            additionalPayment = uiState.additionalPayment,
            fees = uiState.fees,
            total = calculateTotal(
                fees = uiState.fees,
                amount = uiState.amount,
                additionalPayment = uiState.additionalPayment,
                penaltyChargesPortion = loanRepaymentTemplate.penaltyChargesPortion ?: 0.0,
                waivePenalties = uiState.waivePenalties,
            ).toString(),
            accountNumber = uiState.accountNumber,
            externalId = uiState.externalId,
            chequeNumber = uiState.chequeNumber,
            routingCode = uiState.routingCode,
            receiptNumber = uiState.receiptNumber,
            bankNumber = uiState.bankNumber,
            note = uiState.note,
            waivePenalties = uiState.waivePenalties,
            submitPayment = submitPayment,
        )
    }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePickerDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onUpdateRepaymentDate(it)
                        }
                        showDatePickerDialog = false
                    },
                ) { Text(stringResource(Res.string.feature_loan_select_date)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                    },
                ) { Text(stringResource(Res.string.feature_loan_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            style = KptTheme.typography.bodyLarge,
            color = KptTheme.colorScheme.onBackground,
            text = clientName,
        )

        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))

        FarApartTextItem(title = loanProductName, value = loanId.toString())
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_loan_in_arrears),
            value = CurrencyFormatter.format(
                amountInArrears,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_loan_amount_due),
            value = CurrencyFormatter.format(
                loanRepaymentTemplate.amount,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        // Transaction Breakdown Section
        Text(
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            text = stringResource(Res.string.feature_loan_transaction_breakdown),
            modifier = Modifier.padding(top = 10.dp),
        )

        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_principal),
            value = CurrencyFormatter.format(
                loanRepaymentTemplate.principalPortion,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_interest),
            value = CurrencyFormatter.format(
                loanRepaymentTemplate.interestPortion,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_fees),
            value = CurrencyFormatter.format(
                loanRepaymentTemplate.feeChargesPortion,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_penalties),
            value = CurrencyFormatter.format(
                loanRepaymentTemplate.penaltyChargesPortion,
                loanRepaymentTemplate.currency?.code,
                loanRepaymentTemplate.currency?.decimalPlaces,
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        MifosDatePickerTextField(
            modifier = Modifier.fillMaxWidth(),
            value = DateHelper.getDateAsStringFromLong(
                uiState.repaymentDate,
            ),
            label = stringResource(Res.string.feature_loan_repayment_date),
        ) {
            showDatePickerDialog = true
        }

        Spacer(modifier = Modifier.height(16.dp))

        MifosTextFieldDropdown(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.paymentType,
            onValueChanged = { onUpdatePaymentType(it) },
            onOptionSelected = { index, value ->
                onUpdatePaymentTypeWithId(
                    value,
                    loanRepaymentTemplate.paymentTypeOptions?.get(index)?.id ?: 0,
                )
            },
            label = stringResource(Res.string.feature_loan_payment_type),
            options = loanRepaymentTemplate.paymentTypeOptions?.map { it.name } ?: emptyList(),
            readOnly = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.amount,
            onValueChange = { onUpdateAmount(it) },
            label = stringResource(Res.string.feature_loan_amount),
            error = null,
            keyboardType = KeyboardType.Number,
            prefix = loanRepaymentTemplate.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.additionalPayment,
            onValueChange = { onUpdateAdditionalPayment(it) },
            label = stringResource(Res.string.feature_loan_additional_payment),
            error = null,
            keyboardType = KeyboardType.Number,
            prefix = loanRepaymentTemplate.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.fees,
            onValueChange = { onUpdateFees(it) },
            label = stringResource(Res.string.feature_loan_loan_fees),
            error = null,
            keyboardType = KeyboardType.Number,
            prefix = loanRepaymentTemplate.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = calculateTotal(
                fees = uiState.fees,
                amount = uiState.amount,
                additionalPayment = uiState.additionalPayment,
                penaltyChargesPortion = loanRepaymentTemplate.penaltyChargesPortion ?: 0.0,
                waivePenalties = uiState.waivePenalties,
            ).toString(),
            onValueChange = { },
            label = stringResource(Res.string.feature_loan_total),
            error = null,
            readOnly = true,
            prefix = loanRepaymentTemplate.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        MifosCheckBox(
            text = stringResource(Res.string.feature_loan_show_payment_details),
            checked = uiState.showPaymentDetails,
            onCheckChanged = onUpdateShowPaymentDetails,
        )

        if (uiState.showPaymentDetails) {
            Spacer(modifier = Modifier.height(8.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.accountNumber,
                onValueChange = onUpdateAccountNumber,
                label = stringResource(Res.string.feature_loan_account_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.externalId,
                onValueChange = onUpdateExternalId,
                label = stringResource(Res.string.feature_loan_external_id_field),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.chequeNumber,
                onValueChange = onUpdateChequeNumber,
                label = stringResource(Res.string.feature_loan_cheque_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.routingCode,
                onValueChange = onUpdateRoutingCode,
                label = stringResource(Res.string.feature_loan_routing_code),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.receiptNumber,
                onValueChange = onUpdateReceiptNumber,
                label = stringResource(Res.string.feature_loan_receipt_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.bankNumber,
                onValueChange = onUpdateBankNumber,
                label = stringResource(Res.string.feature_loan_bank_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = onUpdateNote,
                label = stringResource(Res.string.feature_loan_note),
                error = null,
                maxLines = 4,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if ((loanRepaymentTemplate.penaltyChargesPortion ?: 0.0) > 0.0) {
            MifosCheckBox(
                text = stringResource(Res.string.feature_loan_waive_penalties),
                checked = uiState.waivePenalties,
                onCheckChanged = onUpdateWaivePenalties,
            )
        } else {
            Text(
                text = stringResource(Res.string.feature_loan_no_penalties_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                modifier = Modifier
                    .heightIn(46.dp),
                onClick = { navigateBack.invoke() },
            ) {
                Text(text = stringResource(Res.string.feature_loan_cancel))
            }

            Button(
                modifier = Modifier
                    .heightIn(46.dp),
                onClick = {
                    if (isAllFieldsValid(
                            amount = uiState.amount,
                            additionalPayment = uiState.additionalPayment,
                            fees = uiState.fees,
                            paymentType = uiState.paymentType,
                        )
                    ) {
                        showConfirmationDialog = true
                    }
                },
            ) {
                Text(text = stringResource(Res.string.feature_loan_review_payment))
            }
        }
    }
}

@Composable
private fun FarApartTextItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            style = KptTheme.typography.bodyLarge,
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            style = KptTheme.typography.bodyLarge,
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShowLoanRepaymentConfirmationDialog(
    onDismiss: () -> Unit,
    loanAccountNumber: String,
    paymentTypeId: String,
    repaymentDate: Long,
    paymentType: String,
    amount: String,
    additionalPayment: String,
    fees: String,
    total: String,
    accountNumber: String,
    externalId: String,
    chequeNumber: String,
    routingCode: String,
    receiptNumber: String,
    bankNumber: String,
    note: String,
    waivePenalties: Boolean,
    submitPayment: (request: LoanRepaymentRequestEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val request = LoanRepaymentRequestEntity(
                        accountNumber = accountNumber.ifBlank { loanAccountNumber },
                        paymentTypeId = paymentTypeId,
                        dateFormat = "dd-MM-yyyy",
                        locale = "en",
                        transactionAmount = total,
                        transactionDate = DateHelper.getDateAsStringFromLong(
                            repaymentDate,
                        ),
                        checkNumber = chequeNumber.ifBlank { null },
                        routingCode = routingCode.ifBlank { null },
                        receiptNumber = receiptNumber.ifBlank { null },
                        bankNumber = bankNumber.ifBlank { null },
                        note = note.ifBlank { null },
                    )
                    submitPayment.invoke(request)
                },
            ) {
                Text(text = stringResource(Res.string.feature_loan_dialog_action_pay_now))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(text = stringResource(Res.string.feature_loan_cancel))
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.feature_loan_review_payment),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(text = stringResource(Res.string.feature_loan_account_number) + " : " + loanAccountNumber)
                Text(
                    text = stringResource(Res.string.feature_loan_repayment_date) + " : " +
                        DateHelper.getDateAsStringFromLong(repaymentDate),
                )
                Text(text = stringResource(Res.string.feature_loan_payment_type) + " : " + paymentType)
                Text(text = stringResource(Res.string.feature_loan_amount) + " : " + amount)
                Text(text = stringResource(Res.string.feature_loan_additional_payment) + " : " + additionalPayment)
                Text(text = stringResource(Res.string.feature_loan_loan_fees) + " : " + fees)
                Text(text = stringResource(Res.string.feature_loan_total) + " : " + total)

                // Show payment details if entered
                if (externalId.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_external_id_field) + " : " + externalId)
                }
                if (chequeNumber.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_cheque_number) + " : " + chequeNumber)
                }
                if (routingCode.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_routing_code) + " : " + routingCode)
                }
                if (receiptNumber.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_receipt_number) + " : " + receiptNumber)
                }
                if (bankNumber.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_bank_number) + " : " + bankNumber)
                }
                if (note.isNotBlank()) {
                    Text(text = stringResource(Res.string.feature_loan_note) + " : " + note)
                }
                if (waivePenalties) {
                    Text(
                        text = stringResource(Res.string.feature_loan_waive_penalties) +
                            " : " + stringResource(Res.string.yes),
                    )
                }
            }
        },
    )
}

/**
 * Calculating the Total of the  Amount, Additional Payment and Fee
 * @return Total of the Amount + Additional Payment + Fees
 */
private fun calculateTotal(
    fees: String,
    amount: String,
    additionalPayment: String,
    penaltyChargesPortion: Double,
    waivePenalties: Boolean,
): Double {
    fun setValue(value: String): Double {
        if (value.isEmpty()) {
            return 0.0
        }
        return try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    val feesValue = setValue(fees)
    val amountValue = setValue(amount)
    val additionalPaymentValue = setValue(additionalPayment)
    val penaltiesValue = if (waivePenalties) 0.0 else penaltyChargesPortion

    return feesValue + amountValue + additionalPaymentValue + penaltiesValue
}

private fun isAllFieldsValid(
    amount: String,
    additionalPayment: String,
    fees: String,
    paymentType: String,
): Boolean {
    val amountValid = amount.trim().toDoubleOrNull()?.let { it > 0 } == true
    // additionalPayment and fees are optional — empty string or 0 is acceptable
    val additionalPaymentValid = additionalPayment.isBlank() ||
        additionalPayment.trim().toDoubleOrNull() != null
    val feesValid = fees.isBlank() ||
        fees.trim().toDoubleOrNull() != null
    return amountValid && additionalPaymentValid && feesValid && paymentType.isNotBlank()
}

private class LoanRepaymentScreenPreviewProvider :
    PreviewParameterProvider<LoanRepaymentUiState> {

    private val samplePaymentTypeOptions = mutableListOf(
        PaymentTypeOptionEntity(
            id = 1,
            name = "Cash",
            description = "Cash payment",
            isCashPayment = true,
            position = 1,
        ),
    )

    private val sampleLoanRepaymentTemplate = LoanRepaymentTemplateEntity(
        loanId = 101,
        date = mutableListOf(2024, 7, 15),
        amount = 1000.0,
        principalPortion = 800.0,
        interestPortion = 150.0,
        feeChargesPortion = 30.0,
        penaltyChargesPortion = 20.0,
        paymentTypeOptions = samplePaymentTypeOptions,
    )

    override val values: Sequence<LoanRepaymentUiState>
        get() = sequenceOf(
            LoanRepaymentUiState(loanRepaymentExistsInDatabase = true),
            LoanRepaymentUiState(loanRepaymentTemplate = sampleLoanRepaymentTemplate),
            LoanRepaymentUiState(error = Res.string.feature_loan_failed_to_load_loan_repayment),
            LoanRepaymentUiState(loanRepaymentExistsInDatabase = false),
            LoanRepaymentUiState(isLoading = true),
            LoanRepaymentUiState(loanRepaymentTemplate = sampleLoanRepaymentTemplate),
        )
}

@Composable
@Preview
private fun PreviewLoanRepaymentScreen(
    @PreviewParameter(LoanRepaymentScreenPreviewProvider::class) loanRepaymentUiState: LoanRepaymentUiState,
) {
    LoanRepaymentScreen(
        loanId = 2,
        clientName = "Ben Kiko",
        loanProductName = "Product name",
        amountInArrears = 23.333,
        loanAccountNumber = 25.toString(),
        uiState = loanRepaymentUiState,
        navigateBack = {},
        snackbarHostState = SnackbarHostState(),
        onRetry = {},
        submitPayment = {},
        onLoanRepaymentDoesNotExistInDatabase = {},
        onUpdatePaymentType = {},
        onUpdatePaymentTypeWithId = { _, _ -> },
        onUpdateAmount = {},
        onUpdateAdditionalPayment = {},
        onUpdateFees = {},
        onUpdateRepaymentDate = {},
        onUpdateShowPaymentDetails = {},
        onUpdateAccountNumber = {},
        onUpdateExternalId = {},
        onUpdateChequeNumber = {},
        onUpdateRoutingCode = {},
        onUpdateReceiptNumber = {},
        onUpdateBankNumber = {},
        onUpdateNote = {},
        onUpdateWaivePenalties = {},
    )
}
