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

package com.pyamsoft.tetherfi.core

import androidx.annotation.CheckResult
import androidx.compose.runtime.Stable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.update

@Singleton
class AppDevEnvironment
@Inject
constructor(
    @Named("in_app_debug") private val inAppDebug: Flow<Boolean>,
) : ExperimentalRuntimeFlags {

  private val mutableGroupFakeEmpty = MutableStateFlow(false)
  private val mutableGroupFakeConnected = MutableStateFlow(false)
  private val mutableGroupFakeError = MutableStateFlow(false)

  private val mutableConnectionFakeEmpty = MutableStateFlow(false)
  private val mutableConnectionFakeConnected = MutableStateFlow(false)
  private val mutableConnectionFakeError = MutableStateFlow(false)

  private val mutableBroadcastFakeError = MutableStateFlow(false)
  private val mutableProxyFakeError = MutableStateFlow(false)
  private val mutableYoloError = MutableStateFlow(false)

  private val mutableSocketBuilderOOMServer = MutableStateFlow(false)
  private val mutableSocketBuilderOOMClient = MutableStateFlow(false)

  val isBroadcastFakeError = mutableBroadcastFakeError.whenInAppDebugEnabled()
  val isProxyFakeError = mutableProxyFakeError.whenInAppDebugEnabled()
  val isYoloError = mutableYoloError.whenInAppDebugEnabled()

  override val isSocketBuilderOOMServer = mutableSocketBuilderOOMServer.whenInAppDebugEnabled()
  override val isSocketBuilderOOMClient = mutableSocketBuilderOOMClient.whenInAppDebugEnabled()

  @get:CheckResult
  val group =
      GroupInfo(
          isEmpty = mutableGroupFakeEmpty.whenInAppDebugEnabled(),
          isConnected = mutableGroupFakeConnected.whenInAppDebugEnabled(),
          isError = mutableGroupFakeError.whenInAppDebugEnabled(),
      )

  @get:CheckResult
  val connection =
      ConnectionInfo(
          isEmpty = mutableConnectionFakeEmpty.whenInAppDebugEnabled(),
          isConnected = mutableConnectionFakeConnected.whenInAppDebugEnabled(),
          isError = mutableConnectionFakeError.whenInAppDebugEnabled(),
      )

  fun updateGroup(
      isEmpty: Boolean? = null,
      isConnected: Boolean? = null,
      isError: Boolean? = null,
  ) {
    isEmpty?.also { mutableGroupFakeEmpty.value = it }
    isConnected?.also { mutableGroupFakeConnected.value = it }
    isError?.also { mutableGroupFakeError.value = it }
  }

  fun updateConnection(
      isEmpty: Boolean? = null,
      isConnected: Boolean? = null,
      isError: Boolean? = null,
  ) {
    isEmpty?.also { mutableConnectionFakeEmpty.value = it }
    isConnected?.also { mutableConnectionFakeConnected.value = it }
    isError?.also { mutableConnectionFakeError.value = it }
  }

  fun updateBroadcast(isError: Boolean) {
    mutableBroadcastFakeError.value = isError
  }

  fun updateProxy(isError: Boolean) {
    mutableProxyFakeError.value = isError
  }

  fun updateYolo(isError: Boolean) {
    mutableYoloError.value = isError
  }

  fun handleToggleSocketBuilderOOMClientEnabled() {
    mutableSocketBuilderOOMClient.update { !it }
  }

  fun handleToggleSocketBuilderOOMServerEnabled() {
    mutableSocketBuilderOOMServer.update { !it }
  }

  @CheckResult
  private fun StateFlow<Boolean>.whenInAppDebugEnabled(): Flow<Boolean> {
    return this.combineTransform(inAppDebug) { flag, isEnabled ->
      if (isEnabled) {
        emit(flag)
      } else {
        emit(false)
      }
    }
  }

  @Stable
  @ConsistentCopyVisibility
  data class GroupInfo
  internal constructor(
      val isEmpty: Flow<Boolean>,
      val isConnected: Flow<Boolean>,
      val isError: Flow<Boolean>,
  )

  @Stable
  @ConsistentCopyVisibility
  data class ConnectionInfo
  internal constructor(
      val isEmpty: Flow<Boolean>,
      val isConnected: Flow<Boolean>,
      val isError: Flow<Boolean>,
  )

  object Defaults {
    const val IS_BROADCAST_FAKE_ERROR_INITIAL_STATE: Boolean = false
    const val IS_PROXY_FAKE_ERROR_INITIAL_STATE: Boolean = false
    const val IS_YOLO_FAKE_ERROR_INITIAL_STATE: Boolean = false

    const val IS_GROUP_FIELD_INITIAL_STATE: Boolean = false
    const val IS_CONNECTION_FIELD_INITIAL_STATE: Boolean = false

    const val IS_SOCKET_FAKE_OOM_CLIENT_INITIAL_STATE: Boolean = false
    const val IS_SOCKET_FAKE_OOM_SERVER_INITIAL_STATE: Boolean = false
  }
}
