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

package com.pyamsoft.tetherfi.main

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.connections.ConnectionComponent
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.info.InfoComponent
import com.pyamsoft.tetherfi.qr.QRCodeComponent
import com.pyamsoft.tetherfi.status.StatusComponent
import dagger.Subcomponent

@Subcomponent
@ActivityScope
internal interface MainComponent {

  @CheckResult fun plusStatus(): StatusComponent.Factory

  @CheckResult fun plusConnection(): ConnectionComponent.Factory

  @CheckResult fun plusInfo(): InfoComponent.Factory

  @CheckResult fun plusQR(): QRCodeComponent.Factory

  fun inject(activity: MainActivity)

  fun inject(injector: MainInjector)

  fun inject(permissionManager: PermissionManager)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult fun create(): MainComponent
  }
}
