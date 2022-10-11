package com.pyamsoft.tetherfi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pyamsoft.pydroid.theme.keylines

@Composable
internal fun BatteryInstructions(
    modifier: Modifier = Modifier,
    appName: String,
    showing: Boolean,
    isIgnored: Boolean,
    keepWakeLock: Boolean,
    onOpenBatterySettings: () -> Unit,
    onToggleBatteryInstructions: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        OutlinedButton(
            onClick = onToggleBatteryInstructions,
        ) {
            Text(
                text = "Improving Performance",
                style = MaterialTheme.typography.h6,
            )
        }

        AnimatedVisibility(
            visible = showing,
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
        ) {
            Column {
                Text(
                    text = "Disable Battery Optimizations to ensure full $appName performance.",
                    style = MaterialTheme.typography.body1,
                )

                if (isIgnored) {
                    Row(
                        modifier = Modifier.padding(top = MaterialTheme.keylines.content),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = MaterialTheme.keylines.baseline),
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Battery Optimizations Disabled",
                            tint = Color.Green,
                        )
                        Text(
                            text = "Battery Optimizations Disabled",
                            style = MaterialTheme.typography.body1,
                        )
                    }
                } else {
                    Button(
                        modifier = Modifier.padding(top = MaterialTheme.keylines.content),
                        onClick = onOpenBatterySettings,
                    ) {
                        Text(
                            text = "Open Battery Settings",
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(top = MaterialTheme.keylines.content * 2),
                    text = "Keep the CPU on to ensure smooth network performance",
                    style = MaterialTheme.typography.body1,
                )

                Row(
                    modifier = Modifier.padding(top = MaterialTheme.keylines.content),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(end = MaterialTheme.keylines.baseline),
                        imageVector = if (keepWakeLock) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = if (keepWakeLock) "CPU kept awake" else "CPU not kept awake",
                        tint = if (keepWakeLock) Color.Green else Color.Red,
                    )
                    if (keepWakeLock) {
                        Text(
                            text = "CPU kept Awake",
                            style = MaterialTheme.typography.body1,
                        )
                    } else {
                        Button(
                            onClick = onToggleKeepWakeLock,
                        ) {
                            Text(
                                text = "Keep CPU Awake",
                            )
                        }
                    }
                }
            }
        }
    }
}