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
import androidclient.feature.loan.generated.resources.feature_loan_error_title
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
import androidclient.feature.loan.generated.resources.feature_loan_payment_success_title
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
import androidclient.feature.loan.generated.resources.label_value_format
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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mifos.core.designsystem.theme.DesignToken
import com.mifos.core.ui.components.MifosCheckBox
import com.mifos.core.ui.components.MifosProgressIndicator
import com.mifos.core.ui.components.MifosProgressIndicatorOverlay
import com.mifos.core.ui.components.MifosStatusDialog
import com.mifos.core.ui.components.ResultStatus
import com.mifos.core.ui.util.EventsEffect
import com.mifos.room.entities.PaymentTypeOptionEntity
import com.mifos.room.entities.templates.loans.LoanRepaymentTemplateEntity
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun LoanRepaymentScreen(
    navigateBack: () -> Unit,
    viewModel: LoanRepaymentViewModel = koinViewModel(),
) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()
    val onAction = remember(viewModel) { viewModel::trySendAction }

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            is LoanRepaymentEvent.NavigateBack -> navigateBack()
        }
    }

    LoanRepaymentScreen(
        uiState = uiState,
        onAction = onAction,
    )
}

@Composable
internal fun LoanRepaymentScreen(
    uiState: LoanRepaymentUiState,
    onAction: (LoanRepaymentAction) -> Unit,
) {
    when (val dialogState = uiState.dialogState) {
        is LoanRepaymentUiState.DialogState.Success -> {
            MifosStatusDialog(
                status = ResultStatus.SUCCESS,
                btnText = stringResource(Res.string.feature_loan_dialog_action_ok),
                successTitle = stringResource(Res.string.feature_loan_payment_success_title),
                successMessage = stringResource(
                    Res.string.feature_loan_payment_success_message,
                    dialogState.resourceId?.toString() ?: "",
                ),
                failureTitle = "",
                failureMessage = "",
                onConfirm = { onAction(LoanRepaymentAction.PaymentSuccess) },
                showAsDialog = true,
            )
        }

        is LoanRepaymentUiState.DialogState.Error -> {
            MifosStatusDialog(
                status = ResultStatus.FAILURE,
                btnText = stringResource(Res.string.feature_loan_dialog_action_ok),
                successTitle = "",
                successMessage = "",
                failureTitle = stringResource(Res.string.feature_loan_error_title),
                failureMessage = dialogState.message,
                onConfirm = { onAction(LoanRepaymentAction.DismissDialog) },
                showAsDialog = true,
            )
        }

        null -> Unit
    }

    if (uiState.showConfirmationDialog) {
        LoanRepaymentConfirmationDialog(
            uiState = uiState,
            onAction = onAction,
        )
    }

    MifosScaffold(
        onBackPressed = { onAction(LoanRepaymentAction.CancelClicked) },
        title = stringResource(Res.string.feature_loan_loan_repayment),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.loanRepaymentTemplate == null && uiState.isLoading -> {
                    MifosProgressIndicator()
                }

                uiState.error != null -> {
                    MifosSweetError(message = stringResource(uiState.error)) {
                        onAction(LoanRepaymentAction.RetryLoading)
                    }
                }

                uiState.loanRepaymentExistsInDatabase -> {
                    AlertDialog(
                        onDismissRequest = { },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onAction(LoanRepaymentAction.CancelClicked)
                                },
                            ) {
                                Text(
                                    text = stringResource(
                                        Res.string.feature_loan_dialog_action_ok,
                                    ),
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(
                                    Res.string.feature_loan_sync_previous_transaction,
                                ),
                                style = KptTheme.typography.titleLarge,
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    Res.string
                                        .feature_loan_dialog_message_sync_transaction,
                                ),
                            )
                        },
                    )
                }

                uiState.loanRepaymentTemplate != null -> {
                    LoanRepaymentContent(
                        uiState = uiState,
                        onAction = onAction,
                    )

                    if (uiState.isLoading) {
                        MifosProgressIndicatorOverlay()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanRepaymentContent(
    uiState: LoanRepaymentUiState,
    onAction: (LoanRepaymentAction) -> Unit,
) {
    val template = uiState.loanRepaymentTemplate ?: return

    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.repaymentDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
        },
    )
    val scrollState = rememberScrollState()

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onAction(LoanRepaymentAction.OnRepaymentDateChange(it))
                        }
                        showDatePickerDialog = false
                    },
                ) { Text(stringResource(Res.string.feature_loan_select_date)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePickerDialog = false },
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
            text = uiState.clientName,
        )

        HorizontalDivider(modifier = Modifier.padding(top = DesignToken.spacing.medium))

        FarApartTextItem(
            title = uiState.loanProductName,
            value = uiState.loanId.toString(),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_loan_in_arrears),
            value = CurrencyFormatter.format(
                uiState.amountInArrears,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_loan_amount_due),
            value = CurrencyFormatter.format(
                template.amount,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = DesignToken.spacing.medium))

        Text(
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onBackground,
            text = stringResource(Res.string.feature_loan_transaction_breakdown),
            modifier = Modifier.padding(top = DesignToken.spacing.medium),
        )

        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_principal),
            value = CurrencyFormatter.format(
                template.principalPortion,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_interest),
            value = CurrencyFormatter.format(
                template.interestPortion,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_fees),
            value = CurrencyFormatter.format(
                template.feeChargesPortion,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )
        FarApartTextItem(
            title = stringResource(Res.string.feature_loan_penalties),
            value = CurrencyFormatter.format(
                template.penaltyChargesPortion,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = DesignToken.spacing.medium))

        MifosDatePickerTextField(
            modifier = Modifier.fillMaxWidth(),
            value = DateHelper.getDateAsStringFromLong(uiState.repaymentDate),
            label = stringResource(Res.string.feature_loan_repayment_date),
        ) {
            showDatePickerDialog = true
        }

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosTextFieldDropdown(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.paymentType,
            onValueChanged = { onAction(LoanRepaymentAction.OnPaymentTypeChange(it)) },
            onOptionSelected = { index, value ->
                onAction(
                    LoanRepaymentAction.OnPaymentTypeSelected(
                        paymentType = value,
                        paymentTypeId = template.paymentTypeOptions
                            ?.get(index)?.id ?: 0,
                    ),
                )
            },
            label = stringResource(Res.string.feature_loan_payment_type),
            options = template.paymentTypeOptions?.map { it.name } ?: emptyList(),
            readOnly = true,
        )

        if (uiState.paymentTypeError != null) {
            Text(
                text = uiState.paymentTypeError,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = KptTheme.spacing.sm,
                    top = KptTheme.spacing.xs,
                ),
            )
        }

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.amount,
            onValueChange = { onAction(LoanRepaymentAction.OnAmountChange(it)) },
            label = stringResource(Res.string.feature_loan_amount),
            keyboardType = KeyboardType.Number,
            prefix = template.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.additionalPayment,
            onValueChange = {
                onAction(LoanRepaymentAction.OnAdditionalPaymentChange(it))
            },
            label = stringResource(Res.string.feature_loan_additional_payment),
            keyboardType = KeyboardType.Number,
            prefix = template.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.fees,
            onValueChange = { onAction(LoanRepaymentAction.OnFeesChange(it)) },
            label = stringResource(Res.string.feature_loan_loan_fees),
            keyboardType = KeyboardType.Number,
            prefix = template.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = CurrencyFormatter.format(
                uiState.total,
                template.currency?.code,
                template.currency?.decimalPlaces,
            ),
            onValueChange = { },
            label = stringResource(Res.string.feature_loan_total),
            readOnly = true,
            prefix = template.currency?.code?.let { code ->
                { Text(text = "$code ") }
            },
        )

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        MifosCheckBox(
            text = stringResource(Res.string.feature_loan_show_payment_details),
            checked = uiState.showPaymentDetails,
            onCheckChanged = {
                onAction(LoanRepaymentAction.OnShowPaymentDetailsChange(it))
            },
        )

        if (uiState.showPaymentDetails) {
            Spacer(modifier = Modifier.height(DesignToken.spacing.small))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.accountNumber,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnAccountNumberChange(it))
                },
                label = stringResource(Res.string.feature_loan_account_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.externalId,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnExternalIdChange(it))
                },
                label = stringResource(Res.string.feature_loan_external_id_field),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.chequeNumber,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnChequeNumberChange(it))
                },
                label = stringResource(Res.string.feature_loan_cheque_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.routingCode,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnRoutingCodeChange(it))
                },
                label = stringResource(Res.string.feature_loan_routing_code),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.receiptNumber,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnReceiptNumberChange(it))
                },
                label = stringResource(Res.string.feature_loan_receipt_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.bankNumber,
                onValueChange = {
                    onAction(LoanRepaymentAction.OnBankNumberChange(it))
                },
                label = stringResource(Res.string.feature_loan_bank_number),
                error = null,
            )

            Spacer(modifier = Modifier.height(DesignToken.spacing.large))

            MifosOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.note,
                onValueChange = { onAction(LoanRepaymentAction.OnNoteChange(it)) },
                label = stringResource(Res.string.feature_loan_note),
                error = null,
                maxLines = 4,
            )
        }

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        if ((template.penaltyChargesPortion ?: 0.0) > 0.0) {
            MifosCheckBox(
                text = stringResource(Res.string.feature_loan_waive_penalties),
                checked = uiState.waivePenalties,
                onCheckChanged = {
                    onAction(LoanRepaymentAction.OnWaivePenaltiesChange(it))
                },
            )
        } else {
            Text(
                text = stringResource(Res.string.feature_loan_no_penalties_found),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(DesignToken.spacing.large))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                modifier = Modifier.heightIn(DesignToken.spacing.dp44),
                onClick = { onAction(LoanRepaymentAction.CancelClicked) },
            ) {
                Text(text = stringResource(Res.string.feature_loan_cancel))
            }

            Button(
                modifier = Modifier.heightIn(DesignToken.spacing.dp44),
                onClick = { onAction(LoanRepaymentAction.ReviewPayment) },
            ) {
                Text(text = stringResource(Res.string.feature_loan_review_payment))
            }
        }

        Spacer(modifier = Modifier.height(KptTheme.spacing.md))
    }
}

@Composable
private fun LoanRepaymentConfirmationDialog(
    uiState: LoanRepaymentUiState,
    onAction: (LoanRepaymentAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(LoanRepaymentAction.DismissConfirmation) },
        confirmButton = {
            TextButton(
                onClick = { onAction(LoanRepaymentAction.SubmitPayment) },
            ) {
                Text(
                    text = stringResource(Res.string.feature_loan_dialog_action_pay_now),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(LoanRepaymentAction.DismissConfirmation) },
            ) {
                Text(text = stringResource(Res.string.feature_loan_cancel))
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.feature_loan_review_payment),
                style = KptTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                LabelValueText(
                    label = stringResource(Res.string.feature_loan_account_number),
                    value = uiState.loanAccountNumber,
                )
                LabelValueText(
                    label = stringResource(Res.string.feature_loan_repayment_date),
                    value = DateHelper.getDateAsStringFromLong(uiState.repaymentDate),
                )
                LabelValueText(
                    label = stringResource(Res.string.feature_loan_payment_type),
                    value = uiState.paymentType,
                )
                LabelValueText(
                    label = stringResource(Res.string.feature_loan_amount),
                    value = uiState.amount,
                )
                if (uiState.additionalPayment.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_additional_payment),
                        value = uiState.additionalPayment,
                    )
                }
                if (uiState.fees.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_loan_fees),
                        value = uiState.fees,
                    )
                }
                LabelValueText(
                    label = stringResource(Res.string.feature_loan_total),
                    value = uiState.total.toString(),
                )

                if (uiState.externalId.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(
                            Res.string.feature_loan_external_id_field,
                        ),
                        value = uiState.externalId,
                    )
                }
                if (uiState.chequeNumber.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_cheque_number),
                        value = uiState.chequeNumber,
                    )
                }
                if (uiState.routingCode.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_routing_code),
                        value = uiState.routingCode,
                    )
                }
                if (uiState.receiptNumber.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_receipt_number),
                        value = uiState.receiptNumber,
                    )
                }
                if (uiState.bankNumber.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_bank_number),
                        value = uiState.bankNumber,
                    )
                }
                if (uiState.note.isNotBlank()) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_note),
                        value = uiState.note,
                    )
                }
                if (uiState.waivePenalties) {
                    LabelValueText(
                        label = stringResource(Res.string.feature_loan_waive_penalties),
                        value = stringResource(Res.string.yes),
                    )
                }
            }
        },
    )
}

@Composable
private fun FarApartTextItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignToken.spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            style = KptTheme.typography.bodyLarge,
            text = title,
            color = KptTheme.colorScheme.onBackground,
        )

        Text(
            style = KptTheme.typography.bodyLarge,
            text = value,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabelValueText(label: String, value: String) {
    Text(
        text = stringResource(Res.string.label_value_format, label, value),
    )
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
            LoanRepaymentUiState(
                error = Res.string.feature_loan_failed_to_load_loan_repayment,
            ),
            LoanRepaymentUiState(isLoading = true),
        )
}

@Composable
@Preview
private fun PreviewLoanRepaymentScreen(
    @PreviewParameter(LoanRepaymentScreenPreviewProvider::class)
    loanRepaymentUiState: LoanRepaymentUiState,
) {
    LoanRepaymentScreen(
        uiState = loanRepaymentUiState,
        onAction = {},
    )
}
