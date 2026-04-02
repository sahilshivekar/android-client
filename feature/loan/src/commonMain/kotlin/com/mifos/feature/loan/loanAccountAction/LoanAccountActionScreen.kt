/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */
package com.mifos.feature.loan.loanAccountAction

import androidclient.feature.loan.generated.resources.Res
import androidclient.feature.loan.generated.resources.feature_loan_action_empty
import androidclient.feature.loan.generated.resources.feature_loan_header_actions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mifos.core.designsystem.theme.DesignToken
import com.mifos.core.designsystem.theme.MifosTypography
import com.mifos.core.ui.components.MifosBreadcrumbNavBar
import com.mifos.core.ui.components.MifosErrorComponent
import com.mifos.core.ui.components.MifosProgressIndicator
import com.mifos.core.ui.components.MifosRowCard
import com.mifos.core.ui.util.EventsEffect
import com.mifos.core.ui.util.TextUtil
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun LoanAccountActionScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onActionSelected: (LoanAccountActionItem, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanAccountActionsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            LoanAccountActionsEvent.NavigateBack -> onNavigateBack.invoke()
            is LoanAccountActionsEvent.NavigateToAction -> {
                onActionSelected(event.action, event.loanId)
            }
        }
    }

    LoanAccountActionContent(
        state = state,
        navController = navController,
        onActionClick = { viewModel.trySendAction(LoanAccountActionsAction.OnActionClick(it)) },
        onRetry = { viewModel.trySendAction(LoanAccountActionsAction.OnRetry) },
        modifier = modifier,
    )
}

@Composable
private fun LoanAccountActionContent(
    state: LoanAccountActionsState,
    navController: NavController,
    onActionClick: (LoanAccountActionItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MifosBreadcrumbNavBar(navController)

        if (state.dialogState == null && !state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KptTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(Res.string.feature_loan_header_actions),
                    style = MifosTypography.labelMediumEmphasized,
                )

                Spacer(Modifier.height(DesignToken.padding.medium))

                if (state.actions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = KptTheme.spacing.xxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.feature_loan_action_empty),
                            style = MifosTypography.bodyMedium,
                            color = KptTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    state.actions.forEach { item ->
                        MifosRowCard(
                            title = stringResource(item.title),
                            imageVector = item.icon,
                            leftValues = listOf(
                                TextUtil(
                                    text = stringResource(item.subTitle),
                                    style = MifosTypography.bodySmall,
                                    color = KptTheme.colorScheme.secondary,
                                ),
                            ),
                            rightValues = emptyList(),
                            modifier = Modifier
                                .clickable { onActionClick(item) }
                                .padding(vertical = DesignToken.padding.medium),
                        )
                    }
                }
                Spacer(Modifier.height(KptTheme.spacing.md))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.dialogState) {
                    is LoanAccountActionsState.DialogState.Loading -> MifosProgressIndicator()
                    is LoanAccountActionsState.DialogState.Error -> {
                        MifosErrorComponent(
                            isNetworkConnected = state.networkConnection,
                            message = stringResource((state.dialogState).message),
                            isRetryEnabled = true,
                            onRetry = onRetry,
                        )
                    }
                    null -> if (state.isLoading) MifosProgressIndicator()
                }
            }
        }
    }
}
