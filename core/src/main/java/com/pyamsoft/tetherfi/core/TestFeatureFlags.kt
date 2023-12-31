package com.pyamsoft.tetherfi.core

import androidx.annotation.VisibleForTesting

@VisibleForTesting
object TestFeatureFlags : FeatureFlags {

  override val isUdpProxyEnabled = false

  override val isTileUiEnabled = false
}
