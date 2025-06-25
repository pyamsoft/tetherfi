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

package com.pyamsoft.tetherfi.server.broadcast

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WiDiConfig
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WiDiConfigImpl
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WifiDirectReceiver
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WifiDirectRegister
import dagger.Binds
import dagger.Module

@Module
abstract class BroadcastServerAppModule {

  @Binds
  @CheckResult
  internal abstract fun bindNetwork(impl: DelegatingBroadcastServer): BroadcastNetwork

  @Binds
  @CheckResult
  internal abstract fun bindNetworkStatus(impl: DelegatingBroadcastServer): BroadcastNetworkStatus

  @Binds
  @CheckResult
  internal abstract fun bindNetworkUpdater(impl: DelegatingBroadcastServer): BroadcastNetworkUpdater

  // Wifi direct
  @Binds
  @CheckResult
  internal abstract fun bindBroadcastObserver(impl: WifiDirectReceiver): BroadcastObserver

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindWidiReceiverRegister(impl: WifiDirectReceiver): WifiDirectRegister

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindConfig(impl: WiDiConfigImpl): WiDiConfig
}
