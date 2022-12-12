package com.pyamsoft.tetherfi.tile

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.tile.TileHandler
import javax.inject.Inject
import timber.log.Timber

internal class ProxyTileService internal constructor() : TileService() {

  @Inject @JvmField internal var tileHandler: TileHandler? = null

  private val tileActivityIntent by
      lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ProxyTileActivity::class.java).apply {
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
    updateTile()
    val tile = qsTile
    if (tile != null) {
      block(tile)
    } else {
      Timber.w("Cannot update tile, no QS Tile")
    }
  }

  private fun updateTile() {
    requestListeningState(
        application,
        ComponentName(
            application,
            ProxyTileService::class.java,
        ),
    )
  }

  private fun setTileStatus(status: RunningStatus) {
    val state: Int
    var description: String
    when (status) {
      is RunningStatus.Error -> {
        state = Tile.STATE_INACTIVE
        description = "Unable to start Hotspot. Click to View Error"
      }
      is RunningStatus.NotRunning -> {
        state = Tile.STATE_INACTIVE
        description = "Click to start Hotspot"
      }
      is RunningStatus.Running -> {
        state = Tile.STATE_ACTIVE
        description = "Hotspot Running"
      }
      is RunningStatus.Starting -> {
        state = Tile.STATE_INACTIVE
        description = "Starting..."
      }
      is RunningStatus.Stopping -> {
        state = Tile.STATE_ACTIVE
        description = "Stopping..."
      }
    }

    // TODO(Peter): Still in BETA
    description = "[BETA]"

    withTile { tile ->
      tile.state = state
      tile.label = description
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
      Timber.d("onStartListening: $handler")
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
      Timber.d("onCreate: $handler")

      handler.bind(
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
    tileHandler?.destroy()

    tileHandler = null
  }
}
