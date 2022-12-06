package com.pyamsoft.tetherfi.ui

import androidx.annotation.CheckResult
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.ZeroSize
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.defaults.ImageDefaults

private data class MaterialCheckableAlphaImpl(
    override val primary: Float,
    override val secondary: Float,
) : MaterialCheckableAlpha

private data class HeightMatcherImpl(
    override val extraHeight: Dp,
    override val onSizeChangedModifier: Modifier,
) : HeightMatcher

private class HeightMatcherGeneratorImpl<T : Any>(
    private val gapHeightGenerator: @Composable (T) -> Dp,
    private val onSizeChangedModifierGenerator: (T) -> Modifier,
) : HeightMatcherGenerator<T> {

  @Composable
  override fun generateFor(item: T): HeightMatcher {
    return HeightMatcherImpl(
        extraHeight = gapHeightGenerator(item),
        onSizeChangedModifier = onSizeChangedModifierGenerator(item),
    )
  }
}

@Composable
@CheckResult
private fun rememberMaterialCheckableColor(condition: Boolean): Color {
  val colors = MaterialTheme.colors
  return remember(
      condition,
      colors,
  ) {
    if (condition) colors.primary else colors.onSurface
  }
}

@Composable
@CheckResult
private fun rememberMaterialCheckableIcon(condition: Boolean): Color {
  val colors = MaterialTheme.colors
  return remember(
      condition,
      colors,
  ) {
    if (condition) colors.success else colors.onSurface
  }
}

@Composable
@CheckResult
private fun rememberMaterialCheckableAlpha(
    isEditable: Boolean,
    condition: Boolean
): MaterialCheckableAlpha {
  val highAlpha = ContentAlpha.high
  val mediumAlpha = ContentAlpha.medium
  val disabledAlpha = ContentAlpha.disabled

  return remember(
      isEditable,
      condition,
      highAlpha,
      mediumAlpha,
      disabledAlpha,
  ) {
    val primary =
        if (isEditable) {
          if (condition) highAlpha else mediumAlpha
        } else disabledAlpha
    val secondary =
        if (isEditable) {
          // High alpha when selected
          if (condition) highAlpha else disabledAlpha
        } else disabledAlpha

    return@remember MaterialCheckableAlphaImpl(primary, secondary)
  }
}

@CheckResult
private fun <T : Any> createGapHeightGenerator(
    density: Density,
    largest: Int,
    heights: Map<T, Int>
): @Composable (T) -> Dp {
  return { item ->
    remember(
        largest,
        density,
        heights,
        item,
    ) {
      val thisHeight: Int = heights[item] ?: return@remember ZeroSize

      val diff = largest - thisHeight
      if (diff < 0) {
        return@remember ZeroSize
      }

      return@remember density.run { diff.toDp() }
    }
  }
}

@CheckResult
private fun <T : Any> createOnSizeChangedModifierGenerator(
    heights: Map<T, Int>,
    setHeights: (Map<T, Int>) -> Unit,
): (T) -> Modifier {
  return { item ->
    Modifier.onSizeChanged { size ->
      // Only do this once, on the initial measure
      val height = size.height
      val entry: Int? = heights[item]

      if (entry == null) {
        setHeights(
            heights.toMutableMap().apply {
              this.set(
                  key = item,
                  value = height,
              )
            },
        )
      }
    }
  }
}

/**
 * MaterialCheckable Alphas
 *
 * Alpha can changed based on editable and selected state
 */
interface MaterialCheckableAlpha {
  val primary: Float
  val secondary: Float
}

/**
 * Given a list of MaterialCheckables in the same parent component, find the one with the largest
 * height.
 *
 * Provide back a Gap height which other components can use to size themselves to the same height as
 * the largest component.
 *
 * Will only work given that the largest component does not update it's height after being measured.
 */
interface HeightMatcherGenerator<T : Any> {

  @Composable fun generateFor(item: T): HeightMatcher
}

/**
 * Given a list of MaterialCheckables in the same parent component, find the one with the largest
 * height.
 *
 * Provide back a Gap height which other components can use to size themselves to the same height as
 * the largest component.
 *
 * Will only work given that the largest component does not update it's height after being measured.
 */
interface HeightMatcher {
  val extraHeight: Dp
  val onSizeChangedModifier: Modifier
}

/**
 * Given a list of items in a parent Composable of different content heights, this remember will
 * create a generator that, when invoked for each item in the list will produce an "extraHeight"
 * which, when applied to each item in the list, will fill smaller items with gap space so that all
 * items become the same height
 */
@Composable
@CheckResult
fun <T : Any> rememberMaterialCheckableHeightMatcherGenerator(): HeightMatcherGenerator<T> {
  val (heights, setHeights) = remember { mutableStateOf(emptyMap<T, Int>()) }

  // Figure out which is the largest and size all other to match
  val largest =
      remember(heights) {
        if (heights.isEmpty()) {
          return@remember 0
        }

        var largest = 0
        for (height in heights.values) {
          if (height > largest) {
            largest = height
          }
        }

        if (largest <= 0) {
          return@remember 0
        }

        return@remember largest
      }

  val density = LocalDensity.current
  return remember(
      density,
      largest,
      heights,
      setHeights,
  ) {
    HeightMatcherGeneratorImpl(
        gapHeightGenerator = createGapHeightGenerator(density, largest, heights),
        onSizeChangedModifierGenerator = createOnSizeChangedModifierGenerator(heights, setHeights),
    )
  }
}

@Composable
fun MaterialCheckable(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    condition: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    /** Hack to make two different cards the same size based on their content */
    extraHeight: Dp = ZeroSize,
) {
  val color = rememberMaterialCheckableColor(condition)
  val iconColor = rememberMaterialCheckableIcon(condition)
  val alphas = rememberMaterialCheckableAlpha(isEditable, condition)

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
            imageVector = Icons.Filled.CheckCircle,
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
