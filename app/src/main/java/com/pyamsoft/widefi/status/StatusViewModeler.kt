package com.pyamsoft.widefi.status

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class StatusViewModeler
@Inject
internal constructor(
    private val state: MutableStatusViewState,
    private val network: WiDiNetwork,
) : AbstractViewModeler<StatusViewState>(state) {

  private fun toggleProxyState(
      scope: CoroutineScope,
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    when (val s = network.getCurrentStatus()) {
      RunningStatus.NotRunning ->
          scope.launch(context = Dispatchers.Main) { network.start(onStart) }
      RunningStatus.Running -> scope.launch(context = Dispatchers.Main) { network.stop(onStop) }
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $s")
      }
    }
  }

  private fun refreshGroupInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) { state.group = network.getGroupInfo() }
  }

  fun watchStatusUpdates(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      network.onProxyStatusChanged { state.proxyStatus = it }
    }

    scope.launch(context = Dispatchers.Main) { network.onStatusChanged { state.wiDiStatus = it } }
  }

  fun handleToggleProxy(
      scope: CoroutineScope,
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    toggleProxyState(
        scope = scope,
        onStart = onStart,
        onStop = onStop,
    )
    refreshGroupInfo(scope = scope)
  }
}
