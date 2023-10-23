/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.broadcast.wifidirect

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetwork
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkUpdater
import com.pyamsoft.tetherfi.server.broadcast.BroadcastObserver
import dagger.Binds
import dagger.Module

@Module
abstract class WifiDirectAppModule {

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetwork(impl: WifiDirectNetwork): BroadcastNetwork

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetworkStatus(impl: WifiDirectNetwork): BroadcastNetworkStatus

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetworkUpdater(impl: WifiDirectNetwork): BroadcastNetworkUpdater

  @Binds
  @CheckResult
  internal abstract fun bindBroadcastObserver(impl: WifiDirectReceiver): BroadcastObserver

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindConfig(impl: WiDiConfigImpl): WiDiConfig

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindWidiReceiverRegister(impl: WifiDirectReceiver): WifiDirectRegister
}
