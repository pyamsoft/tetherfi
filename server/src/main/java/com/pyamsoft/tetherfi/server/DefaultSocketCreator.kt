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

import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.SocketBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

internal class DefaultSocketCreator
internal constructor(
    private val appScope: CoroutineScope,
    private val appEnvironment: AppDevEnvironment,
    private val serverDispatcher: ServerDispatcher,
) : SocketCreator {

  override suspend fun <T> create(
      onError: (Throwable) -> Unit,
      onBuild: suspend (SocketBuilder) -> T,
  ): T {
      val isFakeBuildError = appEnvironment.isSocketBuilderFake.first()
      val isFakeOOM = appEnvironment.isSocketBuilderOOM.first()

      return usingSocketBuilder(
          appScope = appScope,
          isFakeOOM = isFakeOOM,
          isFakeBuildError = isFakeBuildError,
          dispatcher = serverDispatcher.primary,
          onError = onError,
          onBuild = { onBuild(it) },
      )
  }
}
