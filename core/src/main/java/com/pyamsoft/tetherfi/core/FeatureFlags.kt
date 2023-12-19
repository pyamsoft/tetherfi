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

package com.pyamsoft.tetherfi.core

interface FeatureFlags {

  /**
   * Tile UI on Status Screen
   *
   * If we don't get FGS from Google, the TileService may be able to keep our app at a "foreground
   * priority" because as long as the system binds the Tile, the app is kept hot in memory and is
   * allowed to run operations.
   *
   * Technically this is a hack around and Google may close it with their continued war on BG work.
   *
   * https://developer.android.com/guide/components/activities/background-starts
   */
  val isTileUiEnabled: Boolean
}
