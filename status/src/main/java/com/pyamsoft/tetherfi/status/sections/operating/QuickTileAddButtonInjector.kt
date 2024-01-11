/*
 * Copyright 2024 pyamsoft
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

  @JvmField @Inject internal var tileStatus: TileStatus? = null

  @Inject
  @JvmField
  @DrawableRes
  @Named("app_icon_foreground")
  internal var appIconForegroundRes: Int = 0

  @JvmField @Inject internal var tileServiceClass: Class<out TileService>? = null

  override fun onInject(activity: ComponentActivity) {
    StatusObjectGraph.retrieve(activity).inject(this)
  }

  override fun onDispose() {
    tileStatus = null
    tileServiceClass = null
    appIconForegroundRes = 0
  }
}
