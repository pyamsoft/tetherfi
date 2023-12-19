package com.pyamsoft.tetherfi.core

import androidx.annotation.VisibleForTesting

@VisibleForTesting
object TestFeatureFlags : FeatureFlags {
  override val isTileUiEnabled = false
  override val isThreadPerformanceEnabled = false
}
