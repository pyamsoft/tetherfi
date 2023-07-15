package com.pyamsoft.tetherfi.core

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A SharedFlow once collected by definition will never complete. This effectively causes our
 * current Coroutine to permanently suspend as it waits forever for a SharedFlow.collect completion
 * event which will never happen
 */
private val neverEndingFlow = MutableSharedFlow<Nothing>()

/**
 * Hold the coroutine "forever" until it is cancelled
 *
 * Useful for start-stop work like such, where start is controlled by launching the Coroutine and
 * stop is controlled by cancelling i
 *
 * ```kotlin
 * launch {
 *   try {
 *     coroutineScope {
 *       doWorkOnStart()
 *       suspendUntilCancel()
 *     }
 *   } finally {
 *     withContext(NonCancellable) {
 *       doWorkOnCancel()
 *     }
 *   }
 * }
 * ```
 */
public suspend fun suspendUntilCancel(): Nothing {
  neverEndingFlow.collect {}
}
