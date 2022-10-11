package com.pyamsoft.tetherfi.status

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.ZeroSize
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerNetworkBand

@Composable
internal fun NetworkBands(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    band: ServerNetworkBand?,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  val bands = remember { ServerNetworkBand.values() }
  val bandIterator = remember(bands) { bands.withIndex() }

  val density = LocalDensity.current
  val (cardHeights, setCardHeight) = remember { mutableStateOf(emptyMap<ServerNetworkBand, Int>()) }

  Column(
      modifier = modifier,
  ) {
    Row {
      for ((index, b) in bandIterator) {
        val isSelected = remember(b, band) { b == band }

        // Figure out which card is the largest and size all other cards to match
        val largestCard =
            remember(cardHeights) {
              if (cardHeights.isEmpty()) {
                return@remember 0
              }

              var largest = 0
              for (height in cardHeights.values) {
                if (height > largest) {
                  largest = height
                }
              }

              if (largest <= 0) {
                return@remember 0
              }

              return@remember largest
            }

        val gapHeight =
            remember(largestCard, density, cardHeights, b) {
              val cardHeight: Int = cardHeights[b] ?: return@remember ZeroSize

              val diff = largestCard - cardHeight
              if (diff < 0) {
                return@remember ZeroSize
              }

              return@remember density.run { diff.toDp() }
            }

        Card(
            modifier =
                Modifier.weight(1F)
                    .onSizeChanged {
                      // Only do this once, on the initial measure
                      val height = it.height
                      val entry: Int? = cardHeights[b]
                      if (entry == null) {
                        setCardHeight(
                            cardHeights.toMutableMap().apply {
                              this.set(
                                  key = b,
                                  value = height,
                              )
                            },
                        )
                      }
                    }
                    .padding(
                        end =
                            if (index < bands.lastIndex) MaterialTheme.keylines.content
                            else ZeroSize,
                    )
                    .border(
                        width = 2.dp,
                        color =
                            (if (isSelected) MaterialTheme.colors.primary
                                else MaterialTheme.colors.onSurface)
                                .copy(
                                    alpha = 0.6F,
                                ),
                        shape = MaterialTheme.shapes.medium,
                    ),
        ) {
          Row(
              modifier =
                  Modifier.clickable {
                        if (isEditable) {
                          onSelectBand(b)
                        }
                      }
                      .padding(MaterialTheme.keylines.content),
          ) {
            Column {
              Row(
                  verticalAlignment = Alignment.Top,
              ) {
                Text(
                    modifier =
                        Modifier.weight(1F).padding(bottom = MaterialTheme.keylines.baseline),
                    text = b.displayName,
                    style =
                        MaterialTheme.typography.h6.copy(
                            fontWeight = FontWeight.W700,
                            color =
                                if (isSelected) MaterialTheme.colors.primary
                                else MaterialTheme.colors.onSurface,
                        ),
                )

                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = b.name,
                    tint =
                        (if (isSelected) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurface)
                            .copy(
                                alpha = 0.8F,
                            ),
                )
              }

              // Align with the largest card
              if (gapHeight > ZeroSize) {
                Spacer(
                    modifier = Modifier.height(gapHeight),
                )
              }

              Text(
                  text = b.description,
                  style =
                      MaterialTheme.typography.caption.copy(
                          color =
                              if (isSelected) MaterialTheme.colors.primary
                              else
                                  MaterialTheme.colors.onSurface.copy(
                                      alpha = 0.6F,
                                  ),
                          fontWeight = FontWeight.W400,
                      ),
              )
            }
          }
        }
      }
    }
  }
}
