package com.pyamsoft.tetherfi.status.sections.tiiles

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.ui.IconButtonContent
import kotlinx.coroutines.delay

@Composable
internal fun RefreshTile(
    modifier: Modifier = Modifier,
    onRefreshConnection: () -> Unit,
) {
    val hapticManager = LocalHapticManager.current

    StatusTile(
        modifier = modifier,
    ) {
        AttemptRefreshButton(
            onClick = {
                hapticManager?.actionButtonPress()
                onRefreshConnection()
            },
        ) { modifier, iconButton ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = LocalContentColor.current

                iconButton()

                Text(
                    text = "Refresh Hotspot",
                    style =
                    MaterialTheme.typography.caption.copy(
                        color =
                        color.copy(
                            alpha = ContentAlpha.medium,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun AttemptRefreshButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: IconButtonContent,
) {
    val (fakeSpin, setFakeSpin) = remember { mutableStateOf(false) }

    val handleResetFakeSpin by rememberUpdatedState { setFakeSpin(false) }

    val handleClick by rememberUpdatedState {
        setFakeSpin(true)
        onClick()
    }

    LaunchedEffect(fakeSpin) {
        if (fakeSpin) {
            delay(1000L)
            handleResetFakeSpin()
        }
    }

    content(
        Modifier.clickable { handleClick() },
    ) {
        IconButton(
            modifier = modifier,
            onClick = { handleClick() },
        ) {
            val angle by
            rememberInfiniteTransition(
                label = "Refresh",
            )
                .animateFloat(
                    label = "Refresh Spin",
                    initialValue = 0F,
                    targetValue = 360F,
                    animationSpec =
                    infiniteRepeatable(
                        animation =
                        tween(
                            durationMillis = 500,
                            easing = LinearEasing,
                        ),
                        repeatMode = RepeatMode.Restart,
                    ),
                )

            Icon(
                modifier = Modifier.graphicsLayer { rotationZ = if (fakeSpin) angle else 0F },
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}
