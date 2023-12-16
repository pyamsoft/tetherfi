package com.pyamsoft.tetherfi.status.sections.tiiles

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerErrorTile

@Composable
internal fun GroupErrorTile(
    modifier: Modifier = Modifier,
    group: BroadcastNetworkStatus.GroupInfo,
    onShowGroupError: () -> Unit,
) {
    group.cast<BroadcastNetworkStatus.GroupInfo.Error>()?.also {
        StatusTile(
            modifier = modifier,
            color = MaterialTheme.colors.error,
        ) {
            ServerErrorTile(
                onShowError = onShowGroupError,
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
