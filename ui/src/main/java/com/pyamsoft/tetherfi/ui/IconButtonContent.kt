package com.pyamsoft.tetherfi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias IconButtonContent =
    @Composable
    (
        Modifier,
        @Composable () -> Unit,
    ) -> Unit
