package com.pyamsoft.tetherfi.status.sections.operating

import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.tetherfi.status.StatusObjectGraph
import com.pyamsoft.tetherfi.tile.TileStatus
import javax.inject.Inject
import javax.inject.Named

class QuickTileAddButtonInjector : ComposableInjector() {

  @JvmField @Inject
  internal var tileStatus: TileStatus? = null

  @Inject
  @JvmField
  @DrawableRes
  @Named("app_icon_foreground")
  internal var appIconForegroundRes: Int = 0

  @JvmField @Inject
  internal var tileServiceClass: Class<out TileService>? = null

  override fun onInject(activity: ComponentActivity) {
    StatusObjectGraph.retrieve(activity).inject(this)
  }

  override fun onDispose() {
    tileStatus = null
    tileServiceClass = null
    appIconForegroundRes = 0
  }
}