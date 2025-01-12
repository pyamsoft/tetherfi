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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal suspend inline fun relayData(
    scope: CoroutineScope,
    client: TetherClient,
    serverDispatcher: ServerDispatcher,
    proxyInput: ByteReadChannel,
    proxyOutput: ByteWriteChannel,
    internetInput: ByteReadChannel,
    internetOutput: ByteWriteChannel,
    crossinline onReport: suspend (ByteTransferReport) -> Unit,
) {
  val proxyToInternetBytes = MutableStateFlow(0L)
  val internetToProxyBytes = MutableStateFlow(0L)

  val sendReport = suspend {
    val report =
        ByteTransferReport(
            // Reset back to 0 on send
            // If you don't reset, we will keep on sending a higher and higher number
            internetToProxy = internetToProxyBytes.getAndUpdate { 0 },
            proxyToInternet = proxyToInternetBytes.getAndUpdate { 0 },
        )
    onReport(report)
  }

  // Periodically report the transfer status
  val reportJob =
      scope.launch(context = serverDispatcher.sideEffect) {
        while (isActive) {
          delay(5.seconds)
          sendReport()
        }
      }

  try {
    // Exchange data until completed
    val job =
        scope.launch(context = serverDispatcher.primary) {
          // Send data from the internet back to the proxy in a different thread
          talk(
              client = client,
              input = internetInput,
              output = proxyOutput,
              onCopied = { read ->
                if (read > 0) {
                  internetToProxyBytes.update { it + read }
                }
              },
          )
        }

    // Send input from the proxy (clients) to the internet on this thread
    talk(
        client = client,
        input = proxyInput,
        output = internetOutput,
        onCopied = { read ->
          if (read > 0) {
            proxyToInternetBytes.update { it + read }
          }
        },
    )

    // Wait for internet communication to finish
    job.join()
  } finally {
    // After we are done, cancel the periodic report and fire one last report
    reportJob.cancel()
    sendReport()
  }
}
