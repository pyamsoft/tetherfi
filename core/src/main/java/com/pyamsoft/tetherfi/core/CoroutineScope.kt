package com.pyamsoft.tetherfi.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren

/** Attempt to reach into the scope's Job and cancel its children */
fun CoroutineScope.cancelChildren() {
  this.coroutineContext[Job]?.cancelChildren()
}
