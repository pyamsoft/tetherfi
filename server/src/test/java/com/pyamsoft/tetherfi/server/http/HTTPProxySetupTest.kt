/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.server.http

import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.runBlockingWithDelays
import com.pyamsoft.tetherfi.server.setupProxy
import kotlinx.coroutines.delay
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class HTTPProxySetupTest {

  /** It works right? */
  @Test
  fun setupNormal(): Unit = runBlockingWithDelays {
      setupProxy(
          this,
          proxyTypes = listOf(SharedProxy.Type.HTTP),
          isLoggingEnabled = true,
          proxyPort = 23191,
      ) { _, _ ->
          delay(5.seconds)
      }
  }

  /**
   * On files with lower memory like Android 7 or 8, sometimes opening the server socket crashes
   * with Too Many Open Files. We want to catch the problem and show an error in the app, instead of
   * having the app crash and die
   */
  @Test
  fun socketCreatorExceptionIsCaught(): Unit = runBlockingWithDelays {
      setupProxy(
          this,
          proxyTypes = listOf(SharedProxy.Type.HTTP),
          isLoggingEnabled = true,
          proxyPort = 24657,
          testSocketCrash = true,
      ) { _, _ ->
          delay(5.seconds)
      }
  }

  /** We also are prepared to handle when a socket fails to open right? */
  @Test
  fun yoloFailThrows(): Unit = runBlockingWithDelays {
      setupProxy(
          this,
          proxyTypes = listOf(SharedProxy.Type.HTTP),
          isLoggingEnabled = true,
          proxyPort = 35351,
          expectServerFail = true,
          appEnv = { updateYolo(true) },
      ) { _, _ ->
          delay(5.seconds)
      }
  }
}