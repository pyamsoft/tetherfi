package com.pyamsoft.tetherfi.server

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
      "Faster but not compatible with every device. Test test test test",
  )
}
