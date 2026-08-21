/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState


@Composable
fun SnackbarAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable ((SnackbarHostState) -> Unit)? = null,
    text: @Composable ((SnackbarHostState) -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val dialogSnackbarHost = remember { SnackbarHostState() }

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        CompositionLocalProvider(LocalSnackbarHostState provides dialogSnackbarHost) {
            Box(
                modifier = modifier
                    .widthIn(min = 280.dp, max = 560.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
            ) {
                Surface(
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColorFor(containerColor),
                    tonalElevation = tonalElevation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        icon?.let {
                            CompositionLocalProvider(
                                LocalContentColor provides iconContentColor,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 16.dp)
                                        .align(Alignment.CenterHorizontally),
                                ) {
                                    it()
                                }
                            }
                        }
                        title?.let {
                            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                CompositionLocalProvider(
                                    LocalContentColor provides titleContentColor,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .align(
                                                if (icon == null) {
                                                    Alignment.Start
                                                } else {
                                                    Alignment.CenterHorizontally
                                                }
                                            ),
                                    ) {
                                        it(dialogSnackbarHost)
                                    }
                                }
                            }
                        }
                        text?.let {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                CompositionLocalProvider(
                                    LocalContentColor provides textContentColor,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(weight = 1f, fill = false)
                                            .padding(bottom = 24.dp)
                                            .align(Alignment.Start),
                                    ) {
                                        it(dialogSnackbarHost)
                                    }
                                }
                            }
                        }
                        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                dismissButton?.invoke()
                                confirmButton()
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = dialogSnackbarHost,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp)
                        .zIndex(1f),
                )
            }
        }
    }
}
