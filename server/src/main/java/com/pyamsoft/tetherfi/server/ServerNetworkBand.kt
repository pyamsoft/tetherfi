package com.pyamsoft.tetherfi.server

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
enum class ServerNetworkBand(
    val displayName: String,
    val description: String,
) {
  LEGACY(
      "2.4GHz",
      "Slower but compatible with every device.",
  ),
  MODERN(
      "5GHz",
      "Faster but not compatible with every device.",
  )
}
