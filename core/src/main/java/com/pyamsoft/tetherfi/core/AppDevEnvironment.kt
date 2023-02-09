package com.pyamsoft.tetherfi.core

import androidx.annotation.CheckResult
import androidx.compose.runtime.Stable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AppDevEnvironment @Inject internal constructor() {

  private val isGroupFakeEmpty = MutableStateFlow(false)
  private val isGroupFakeConnected = MutableStateFlow(false)
  private val isGroupFakeError = MutableStateFlow(false)

  private val isConnectionFakeEmpty = MutableStateFlow(false)
  private val isConnectionFakeConnected = MutableStateFlow(false)
  private val isConnectionFakeError = MutableStateFlow(false)

  @get:CheckResult
  val group =
      GroupInfo(
          isEmpty = isGroupFakeEmpty,
          isConnected = isGroupFakeConnected,
          isError = isGroupFakeError,
      )

  @get:CheckResult
  val connection =
      ConnectionInfo(
          isEmpty = isConnectionFakeEmpty,
          isConnected = isConnectionFakeConnected,
          isError = isConnectionFakeError,
      )

  fun updateGroup(
      isEmpty: Boolean? = null,
      isConnected: Boolean? = null,
      isError: Boolean? = null,
  ) {
    isEmpty?.also { isGroupFakeEmpty.value = it }
    isConnected?.also { isGroupFakeConnected.value = it }
    isError?.also { isGroupFakeError.value = it }
  }

  fun updateConnection(
      isEmpty: Boolean? = null,
      isConnected: Boolean? = null,
      isError: Boolean? = null,
  ) {
    isEmpty?.also { isConnectionFakeEmpty.value = it }
    isConnected?.also { isConnectionFakeConnected.value = it }
    isError?.also { isConnectionFakeError.value = it }
  }

  @Stable
  data class GroupInfo
  internal constructor(
      val isEmpty: StateFlow<Boolean>,
      val isConnected: StateFlow<Boolean>,
      val isError: StateFlow<Boolean>,
  )

  @Stable
  data class ConnectionInfo
  internal constructor(
      val isEmpty: StateFlow<Boolean>,
      val isConnected: StateFlow<Boolean>,
      val isError: StateFlow<Boolean>,
  )
}
