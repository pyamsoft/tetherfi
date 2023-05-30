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

package com.pyamsoft.tetherfi.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.tile.TileHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

internal class ProxyTileService internal constructor() : TileService() {

  @Inject @JvmField internal var tileHandler: TileHandler? = null

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
    )
  }

  private val tileActivityIntent by
      lazy(LazyThreadSafetyMode.NONE) {
        Intent(application, ProxyTileActivity::class.java).apply {
          flags =
              Intent.FLAG_ACTIVITY_SINGLE_TOP or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP or
                  Intent.FLAG_ACTIVITY_NEW_TASK
        }
      }

  private inline fun ensureUnlocked(crossinline block: () -> Unit) {
    if (isLocked) {
      unlockAndRun { block() }
    } else {
      block()
    }
  }

  private inline fun withTile(crossinline block: (Tile) -> Unit) {
    val tile = qsTile
    if (tile != null) {
      block(tile)

      // Make sure we call this or nothing actually happens
      tile.updateTile()
    } else {
      Timber.w("Cannot update tile, no QS Tile, try requesting LS update")
      updateTile(application)
    }
  }

  private fun setTileStatus(status: RunningStatus) {
    val state: Int
    var title: String
    val description: String
    when (status) {
      is RunningStatus.Error -> {
        state = Tile.STATE_INACTIVE
        title = "ERROR"
        description = "Unable to start Hotspot. Click to View Error"
      }
      is RunningStatus.NotRunning -> {
        state = Tile.STATE_INACTIVE
        title = getString(R.string.app_name)
        description = "Click to start Hotspot"
      }
      is RunningStatus.Running -> {
        state = Tile.STATE_ACTIVE
        title = getString(R.string.app_name)
        description = "Hotspot Running"
      }
      is RunningStatus.Starting -> {
        state = Tile.STATE_INACTIVE
        title = getString(R.string.app_name)
        description = "Starting..."
      }
      is RunningStatus.Stopping -> {
        state = Tile.STATE_ACTIVE
        title = getString(R.string.app_name)
        description = "Stopping..."
      }
    }

    // TODO beta still
    title = "TetherFi [BETA]"

    withTile { tile ->
      tile.state = state
      tile.label = title
      tile.contentDescription = description

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        tile.stateDescription = description
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        tile.subtitle = description
      }
    }
  }

  private fun handleNetworkErrorState(err: RunningStatus.Error) {
    setTileStatus(err)
  }

  private fun handleNetworkNotRunningState() {
    setTileStatus(RunningStatus.NotRunning)
  }

  private fun handleNetworkRunningState() {
    setTileStatus(RunningStatus.Running)
  }

  private fun handleNetworkStartingState() {
    setTileStatus(RunningStatus.Starting)
  }

  private fun handleNetworkStoppingState() {
    setTileStatus(RunningStatus.Stopping)
  }

  private fun withHandler(block: (TileHandler) -> Unit) {
    if (tileHandler == null) {
      // Need to constantly re-bind here because each time this is called, the tile service may have
      // changed
      //
      // We also must inject via our own SubComponent to ensure that Dagger re-creates and
      // re-injects each time. If we inject directly from the AppComponent, Dagger internally tracks
      // the injection and does not inject again even though the service lifecycle requires it.
      Timber.d("Injecting handler!")
      ObjectGraph.ApplicationScope.retrieve(this).plusTile().create().inject(this)
    }

    block(tileHandler.requireNotNull())
  }

  override fun onClick() {
    Timber.d("Tile Clicked!")
    ensureUnlocked {
      Timber.d("Start TileActivity!")
      startActivityAndCollapse(tileActivityIntent)
    }
  }

  override fun onStartListening() {
    withHandler { handler ->
      when (val status = handler.getNetworkStatus()) {
        is RunningStatus.Error -> handleNetworkErrorState(status)
        is RunningStatus.NotRunning -> handleNetworkNotRunningState()
        is RunningStatus.Running -> handleNetworkRunningState()
        is RunningStatus.Starting -> handleNetworkStartingState()
        is RunningStatus.Stopping -> handleNetworkStoppingState()
      }
    }
  }

  override fun onCreate() {
    super.onCreate()

    withHandler { handler ->
      handler.bind(
          scope = scope,
          onNetworkError = { err -> handleNetworkErrorState(err) },
          onNetworkNotRunning = { handleNetworkNotRunningState() },
          onNetworkRunning = { handleNetworkRunningState() },
          onNetworkStarting = { handleNetworkStartingState() },
          onNetworkStopping = { handleNetworkStoppingState() },
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    tileHandler = null
  }

  companion object {

    @JvmStatic
    fun updateTile(context: Context) {
      val appContext = context.applicationContext
      requestListeningState(
          appContext,
          ComponentName(
              appContext,
              ProxyTileService::class.java,
          ),
      )
    }
  }
}
