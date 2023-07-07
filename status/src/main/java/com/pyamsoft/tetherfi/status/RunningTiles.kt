package com.pyamsoft.tetherfi.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.IconButtonContent
import com.pyamsoft.tetherfi.ui.ServerErrorTile
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.icons.QrCode
import kotlinx.coroutines.delay

@Composable
internal fun RunningTiles(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
) {
  val group by serverViewState.group.collectAsState()
  val connection by serverViewState.connection.collectAsState()

  val isQREnabled =
      remember(
          connection,
          group,
      ) {
        connection is WiDiNetworkStatus.ConnectionInfo.Connected &&
            group is WiDiNetworkStatus.GroupInfo.Connected
      }

  val hapticManager = LocalHapticManager.current

  Column(
      modifier = modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Tile(
          modifier = Modifier.weight(1F),
      ) {
        AttemptRefresh(
            onClick = {
              hapticManager?.actionButtonPress()
              onRefreshConnection()
            },
        ) { modifier, iconButton ->
          Row(
              modifier = Modifier.fillMaxWidth().then(modifier),
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

      Spacer(
          modifier = Modifier.width(MaterialTheme.keylines.content),
      )

      Tile(
          modifier = Modifier.weight(1F),
          enabled = isQREnabled,
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = isQREnabled) { onShowQRCode() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
          val color = LocalContentColor.current

          IconButton(
              onClick = { onShowQRCode() },
              enabled = isQREnabled,
          ) {
            Icon(
                imageVector = Icons.Filled.QrCode,
                contentDescription = "QR Code",
                tint =
                    MaterialTheme.colors.primary.copy(
                        alpha = if (isQREnabled) ContentAlpha.high else ContentAlpha.disabled,
                    ),
            )
          }

          Text(
              text = "View QR Code",
              style =
                  MaterialTheme.typography.caption.copy(
                      color =
                          color.copy(
                              alpha =
                                  if (isQREnabled) ContentAlpha.medium else ContentAlpha.disabled,
                          ),
                  ),
          )
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      (connection as? WiDiNetworkStatus.ConnectionInfo.Error)?.also {
        Tile(
            modifier = Modifier.weight(1F),
            color = MaterialTheme.colors.error,
        ) {
          ServerErrorTile(
              onShowError = onShowNetworkError,
          ) { modifier, iconButton ->
            Row(
                modifier = Modifier.fillMaxWidth().then(modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              val color = LocalContentColor.current

              iconButton()

              Text(
                  text = "Network Error",
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

      if (connection is WiDiNetworkStatus.ConnectionInfo.Error &&
          group is WiDiNetworkStatus.GroupInfo.Error) {
        Spacer(
            modifier = Modifier.width(MaterialTheme.keylines.content),
        )
      }

      (group as? WiDiNetworkStatus.GroupInfo.Error)?.also {
        Tile(
            modifier = Modifier.weight(1F),
            color = MaterialTheme.colors.error,
        ) {
          ServerErrorTile(
              onShowError = onShowHotspotError,
          ) { modifier, iconButton ->
            Row(
                modifier = Modifier.fillMaxWidth().then(modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              val color = LocalContentColor.current

              iconButton()

              Text(
                  text = "Hotspot Error",
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
    }
  }
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color =
                  color.copy(
                      alpha = if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
                  ),
              shape = MaterialTheme.shapes.medium,
          ),
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.Elevation,
  ) {
    content()
  }
}

@Composable
private fun AttemptRefresh(
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
          rememberInfiniteTransition()
              .animateFloat(
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
