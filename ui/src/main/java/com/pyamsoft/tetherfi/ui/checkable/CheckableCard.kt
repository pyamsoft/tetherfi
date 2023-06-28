package com.pyamsoft.tetherfi.ui.checkable

import androidx.annotation.CheckResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.theme.success
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.defaults.ImageDefaults
import com.pyamsoft.pydroid.ui.icons.RadioButtonUnchecked
import com.pyamsoft.pydroid.ui.theme.ZeroSize

@Composable
@CheckResult
fun rememberCheckableColor(
    condition: Boolean,
    selectedColor: Color,
): State<Color> {
  val unselectedColor = MaterialTheme.colors.onSurface
  val color =
      remember(
          condition,
          unselectedColor,
          selectedColor,
      ) {
        if (condition) selectedColor else unselectedColor
      }
  return animateColorAsState(color)
}

@Composable
@CheckResult
internal fun rememberCheckableIconColor(condition: Boolean): Color {
  val unselectedColor = MaterialTheme.colors.onSurface
  val selectedColor = MaterialTheme.colors.success
  return remember(
      condition,
      unselectedColor,
      selectedColor,
  ) {
    if (condition) selectedColor else unselectedColor
  }
}

/** Fancy checkable with Material Design ish elements */
@Composable
fun CheckableCard(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    condition: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    /** Hack to make two different cards the same size based on their content */
    extraHeight: Dp = ZeroSize,
) {

  CheckableCard(
      modifier = modifier,
      isEditable = isEditable,
      condition = condition,
      title = title,
      description = description,
      selectedColor = MaterialTheme.colors.primary,
      extraHeight = extraHeight,
      onClick = onClick,
  )
}

@Composable
private fun CheckableCard(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    condition: Boolean,
    title: String,
    description: String,
    selectedColor: Color,
    extraHeight: Dp,
    onClick: () -> Unit,
) {
  val iconColor = rememberCheckableIconColor(condition)
  val alphas = rememberCheckableAlpha(isEditable)
  val color by rememberCheckableColor(condition, selectedColor)

  val checkIcon =
      remember(condition) {
        if (condition) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked
      }

  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color = color.copy(alpha = alphas.secondary),
              shape = MaterialTheme.shapes.medium,
          ),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier =
            Modifier.clickable(enabled = isEditable) { onClick() }
                .padding(MaterialTheme.keylines.content),
    ) {
      Row(
          verticalAlignment = Alignment.Top,
      ) {
        Text(
            modifier = Modifier.weight(1F).padding(bottom = MaterialTheme.keylines.baseline),
            text = title,
            style =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W700,
                    color = color.copy(alpha = alphas.primary),
                ),
        )

        Icon(
            modifier = Modifier.size(ImageDefaults.IconSize),
            imageVector = checkIcon,
            contentDescription = title,
            tint = iconColor.copy(alpha = alphas.secondary),
        )
      }

      // Align with the largest card
      if (extraHeight > ZeroSize) {
        Spacer(
            modifier = Modifier.height(extraHeight),
        )
      }

      Text(
          text = description,
          style =
              MaterialTheme.typography.caption.copy(
                  color = MaterialTheme.colors.onSurface.copy(alpha = alphas.secondary),
                  fontWeight = FontWeight.W400,
              ),
      )
    }
  }
}
