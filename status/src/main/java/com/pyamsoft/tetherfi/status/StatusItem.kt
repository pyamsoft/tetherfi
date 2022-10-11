package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun StatusItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueStyle: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style =
            MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.W700,
                color =
                MaterialTheme.colors.onSurface.copy(
                    alpha = ContentAlpha.medium,
                )
            ),
        )
        Text(
            text = value,
            style = valueStyle,
            color = color,
        )
    }
}