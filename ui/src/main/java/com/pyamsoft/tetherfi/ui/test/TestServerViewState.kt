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

package com.pyamsoft.tetherfi.ui.test

import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.TestOnly

@TestOnly
@VisibleForTesting
enum class TestServerState {
  EMPTY,
  CONNECTED,
  ERROR,
}

@TestOnly @VisibleForTesting const val TEST_SSID = "TEST"

@TestOnly @VisibleForTesting const val TEST_PASSWORD = "testing123"

@TestOnly @VisibleForTesting const val TEST_PORT = 9999

@TestOnly @VisibleForTesting const val TEST_HOSTNAME = "127.0.0.1"

@TestOnly
@CheckResult
@VisibleForTesting
fun makeTestServerState(state: TestServerState): ServerViewState =
    when (state) {
      TestServerState.EMPTY ->
          object : ServerViewState {
            override val group = MutableStateFlow(BroadcastNetworkStatus.GroupInfo.Empty)
            override val connection = MutableStateFlow(BroadcastNetworkStatus.ConnectionInfo.Empty)
            override val port = MutableStateFlow(TEST_PORT)
          }
      TestServerState.CONNECTED ->
          object : ServerViewState {
            override val group =
                MutableStateFlow(
                    BroadcastNetworkStatus.GroupInfo.Connected(
                        ssid = TEST_SSID,
                        password = TEST_PASSWORD,
                    ))
            override val connection =
                MutableStateFlow(
                    BroadcastNetworkStatus.ConnectionInfo.Connected(hostName = TEST_HOSTNAME))
            override val port = MutableStateFlow(TEST_PORT)
          }
      TestServerState.ERROR ->
          object : ServerViewState {
            override val group =
                MutableStateFlow(
                    BroadcastNetworkStatus.GroupInfo.Error(
                        error = RuntimeException("Test Group Error")))
            override val connection =
                MutableStateFlow(
                    BroadcastNetworkStatus.ConnectionInfo.Error(
                        error = RuntimeException("Test Connection Error")))
            override val port = MutableStateFlow(TEST_PORT)
          }
    }
