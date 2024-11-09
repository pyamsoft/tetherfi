/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server

import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class ProxySetupTest {

  @Test
  fun setupNormal(): Unit = runBlockingWithDelays {
    setupProxy(
        this,
        isLoggingEnabled = true,
        proxyPort = 5553,
    ) {
      delay(5.seconds)
    }
  }

  @Test
  fun socketCreatorExceptionIsCaught(): Unit = runBlockingWithDelays {
    setupProxy(
        this,
        isLoggingEnabled = true,
        proxyPort = 5555,
        testSocketCrash = true,
    ) {
      delay(5.seconds)
    }
  }

  /** We also are prepared to handle when a socket fails to open right? */
  @Test
  fun yoloFailThrows(): Unit = runBlockingWithDelays {
    setupProxy(
        this,
        isLoggingEnabled = true,
        proxyPort = 5554,
        expectServerFail = true,
        appEnv = { updateYolo(true) },
    ) {
      delay(5.seconds)
    }
  }
}
