/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.ui.checkable

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.pyamsoft.pydroid.ui.theme.ZeroSize

@ConsistentCopyVisibility
data class HeightMatcher
internal constructor(
    val extraHeight: Dp,
    val onSizeChangedModifier: Modifier,
)

@ConsistentCopyVisibility
data class HeightMatcherGenerator<T : Any>
internal constructor(
    private val gapHeightGenerator: @Composable (T) -> Dp,
    private val onSizeChangedModifierGenerator: (T) -> Modifier,
) {

  @Composable
  fun generateFor(item: T): HeightMatcher {
    return HeightMatcher(
        extraHeight = gapHeightGenerator(item),
        onSizeChangedModifier = onSizeChangedModifierGenerator(item),
    )
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
 * Given a list of items in a parent Composable of different content heights, this remember will
 * create a generator that, when invoked for each item in the list will produce an "extraHeight"
 * which, when applied to each item in the list, will fill smaller items with gap space so that all
 * items become the same height
 */
@Composable
@CheckResult
fun <T : Any> rememberHeightMatcherGenerator(): HeightMatcherGenerator<T> {
  val (heights, setHeights) = remember { mutableStateOf(emptyMap<T, Int>()) }

  val handleSetHeight by rememberUpdatedState(setHeights)

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
  ) {
    HeightMatcherGenerator(
        gapHeightGenerator = createGapHeightGenerator(density, largest, heights),
        onSizeChangedModifierGenerator =
            createOnSizeChangedModifierGenerator(
                heights = heights,
                setHeights = { handleSetHeight(it) },
            ),
    )
  }
}
