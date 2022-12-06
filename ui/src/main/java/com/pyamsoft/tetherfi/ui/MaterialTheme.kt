package com.pyamsoft.tetherfi.ui

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

private val GREEN = Color(0xFF4CAF50)

/** Provide a Theme color for Success */
@Suppress("unused")
val Colors.success: Color
  get() = GREEN

/** Provide a Theme color for OnSuccess */
@Suppress("unused")
val Colors.onSuccess: Color
  get() = this.onError
