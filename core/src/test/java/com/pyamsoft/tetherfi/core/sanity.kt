package com.pyamsoft.tetherfi.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class JVMSanity {

  @Test fun sanity() = runTest { assertEquals(2 + 2, 4) }
}
