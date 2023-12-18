package com.pyamsoft.tetherfi.core

import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
object TestFeatureFlags : FeatureFlags {
  override val isTileUiEnabled = false
  override val isThreadPerformanceEnabled = false
}
