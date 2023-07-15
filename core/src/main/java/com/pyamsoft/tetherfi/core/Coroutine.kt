package com.pyamsoft.tetherfi.core

import kotlinx.coroutines.flow.MutableSharedFlow

private val neverEndingFlow = MutableSharedFlow<Nothing>()

/**
 * Hold the coroutine "forever" until it is cancelled
 *
 * Useful for start-stop work like such
 *
 * launch { try { coroutineScope { doWorkOnStart()
 *
 *        suspendUntilCancel()
 *      }
 *    } finally {
 *      withContext(NonCancellable) {
 *        doWorkOnCancel()
 *      }
 *    }
 *
 * }
 */
suspend fun suspendUntilCancel() {
  neverEndingFlow.collect {}
}
