package com.pyamsoft.tetherfi.service.tile

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.TetherFiComponent
import com.pyamsoft.tetherfi.main.MainActivity
import javax.inject.Inject
import timber.log.Timber

internal class ProxyTileService internal constructor() : TileService() {

  private val mainActivityIntent by
      lazy(LazyThreadSafetyMode.NONE) {
        Intent(application, MainActivity::class.java).apply {
          flags =
              Intent.FLAG_ACTIVITY_SINGLE_TOP or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP or
                  Intent.FLAG_ACTIVITY_NEW_TASK
        }
      }

  @Inject @JvmField internal var tileHandler: TileHandler? = null

  @CheckResult
  private fun createAlertDialog(message: String): Dialog {
    val appName = getString(R.string.app_name)
    return AlertDialog.Builder(this)
        .setTitle("$appName Networking Error")
        .setMessage(message)
        .setPositiveButton("Open $appName") { dialog, _ ->
          dialog.dismiss()
          ensureUnlocked { startActivityAndCollapse(mainActivityIntent) }
        }
        .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
        .create()
  }

  private inline fun withHandler(block: (TileHandler) -> Unit) {
    if (tileHandler == null) {
      Timber.d("Inject a new TileHandler")
      Injector.obtainFromApplication<TetherFiComponent>(application)
          .plusTileService()
          .create(
              getTile = { qsTile },
              showDialog = { message -> showDialog(createAlertDialog(message)) },
          )
          .inject(this)
    }

    block(tileHandler.requireNotNull())
  }

  private inline fun ensureUnlocked(crossinline block: () -> Unit) {
    if (isLocked) {
      unlockAndRun { block() }
    } else {
      block()
    }
  }

  override fun onClick() = withHandler { handler ->
    Timber.d("Tile clicked!")
    ensureUnlocked { handler.toggleProxyNetwork(qsTile) }
  }

  override fun onStartListening() = withHandler { handler ->
    Timber.d("Tile has started listening")
    handler.sync(qsTile)
  }

  override fun onStopListening() = withHandler { handler ->
    Timber.d("Tile has stopped listening")
    handler.sync(qsTile)
  }

  override fun onCreate() {
    super.onCreate()

    withHandler { handler ->
      Timber.d("Tile is created, bind!")
      handler.bind()
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.d("Tile is destroyed!")
    tileHandler?.destroy()
    tileHandler = null
  }
}
