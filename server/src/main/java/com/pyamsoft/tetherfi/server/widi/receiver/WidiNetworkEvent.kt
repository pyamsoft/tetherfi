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

package com.pyamsoft.tetherfi.server.widi.receiver

sealed class WidiNetworkEvent {
  object WifiEnabled : WidiNetworkEvent()

  object WifiDisabled : WidiNetworkEvent()

  object PeersChanged : WidiNetworkEvent()

  object ThisDeviceChanged : WidiNetworkEvent()

  object DiscoveryChanged : WidiNetworkEvent()

  data class ConnectionChanged internal constructor(val ip: String) : WidiNetworkEvent()
}
