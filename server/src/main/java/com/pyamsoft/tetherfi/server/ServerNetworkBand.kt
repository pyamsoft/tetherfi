package com.pyamsoft.tetherfi.server

enum class ServerNetworkBand(val description: String) {
  LEGACY("2.4GHz - Slower but compatible with almost all devices."),
  MODERN("5GHz - Faster but not compatible with every device.")
}
