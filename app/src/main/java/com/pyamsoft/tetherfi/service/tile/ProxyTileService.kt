package com.pyamsoft.tetherfi.service.tile

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.tile.ProxyTileActivity
import javax.inject.Inject

internal class ProxyTileService internal constructor() : TileService() {

  private val tileActivityIntent by
      lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ProxyTileActivity::class.java).apply {
          flags =
              Intent.FLAG_ACTIVITY_SINGLE_TOP or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP or
                  Intent.FLAG_ACTIVITY_NEW_TASK
        }
      }

  @Inject @JvmField internal var handler: TileHandler? = null

  private inline fun ensureUnlocked(crossinline block: () -> Unit) {
    if (isLocked) {
      unlockAndRun { block() }
    } else {
      block()
    }
  }

  private inline fun withTile(crossinline block: (Tile) -> Unit) {
    requestTileUpdate()

    val tile = qsTile
    if (tile != null) {
      block(tile)
    }

    requestTileUpdate()
  }

  private fun requestTileUpdate() {
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
    val description: String
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

    withTile { tile ->
      tile.state = state
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

  override fun onClick() = ensureUnlocked { startActivityAndCollapse(tileActivityIntent) }

  override fun onStartListening() {
    handler
        .requireNotNull()
        .sync(
            onNetworkError = { err -> handleNetworkErrorState(err) },
            onNetworkNotRunning = { handleNetworkNotRunningState() },
            onNetworkRunning = { handleNetworkRunningState() },
            onNetworkStarting = { handleNetworkStartingState() },
            onNetworkStopping = { handleNetworkStoppingState() },
        )
  }

  override fun onCreate() {
    super.onCreate()

    ObjectGraph.ApplicationScope.retrieve(this).inject(this)

    handler
        .requireNotNull()
        .bind(
            onNetworkError = { err -> handleNetworkErrorState(err) },
            onNetworkNotRunning = { handleNetworkNotRunningState() },
            onNetworkRunning = { handleNetworkRunningState() },
            onNetworkStarting = { handleNetworkStartingState() },
            onNetworkStopping = { handleNetworkStoppingState() },
        )
  }

  override fun onDestroy() {
    super.onDestroy()
    handler?.destroy()
    handler = null
  }
}
