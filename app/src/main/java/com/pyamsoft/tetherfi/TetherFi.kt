/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.bootstrap.libraries.OssLicenses
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.ui.debug.InAppDebugStatus
import com.pyamsoft.pydroid.ui.installPYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.core.TERMS_CONDITIONS_URL
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TetherFi : Application() {

  @CheckResult
  private fun initPYDroid(): ModuleProvider {
    val url = "https://github.com/pyamsoft/tetherfi"

    return installPYDroid(
        PYDroid.Parameters(
            viewSourceUrl = url,
            bugReportUrl = "$url/issues",
            privacyPolicyUrl = PRIVACY_POLICY_URL,
            termsConditionsUrl = TERMS_CONDITIONS_URL,
            version = BuildConfig.VERSION_CODE,
            logger = createLogger(),
        ),
    )
  }

  private fun installObjectGraph(component: TetherFiComponent) {
    ObjectGraph.ApplicationScope.install(this, component)
  }

  private fun installComponent(
      scope: CoroutineScope,
      moduleProvider: ModuleProvider,
      inAppDebugStatus: InAppDebugStatus,
  ) {
    val mods = moduleProvider.get()
    val component =
        DaggerTetherFiComponent.factory()
            .create(
                debug = isDebugMode(),
                inAppDebug = inAppDebugStatus.listenForInAppDebuggingEnabled(),
                scope = scope,
                application = this,
                imageLoader = mods.imageLoader(),
                theming = mods.theming(),
                enforcer = mods.enforcer(),
            )

    installObjectGraph(component)
  }

  override fun onCreate() {
    super.onCreate()
    val modules = initPYDroid()

    val scope =
        CoroutineScope(
            context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
        )

    val inAppDebugStatus = modules.get().inAppDebugStatus()
    installLogger(
        scope = scope,
        inAppDebugStatus = inAppDebugStatus,
    )

    installComponent(
        scope = scope,
        moduleProvider = modules,
        inAppDebugStatus = inAppDebugStatus,
    )
    addLibraries()
  }

  companion object {

    @JvmStatic
    private fun addLibraries() {
      OssLibraries.apply {
        usingNotify = true
        usingArch = true
        usingUi = true
      }

      OssLibraries.add(
          "Dagger",
          "https://github.com/google/dagger",
          "A fast dependency injector for Android and Java.",
      )

      OssLibraries.add(
          "LeakCanary",
          "https://github.com/square/leakcanary",
          "A memory leak detection library for Android.",
      )

      OssLibraries.add(
          "Timber",
          "https://github.com/JakeWharton/timber",
          "A logger with a small, extensible API which provides utility on top of Android's normal Log class.",
      )

      OssLibraries.add(
          "KSP",
          "https://github.com/google/ksp",
          "Kotlin Symbol Processing API",
      )

      OssLibraries.add(
          "Ktor",
          "https://github.com/ktorio/ktor",
          "Framework for quickly creating connected applications in Kotlin with minimal effort",
      )

      OssLibraries.add(
          "Ktor (pyamsoft fork)",
          "https://github.com/pyamsoft/ktor",
          "A fork of Ktor that adds support for Socket customization (used for the 'Preferred Network' feature)",
      )

      OssLibraries.add(
          "AndroidX Appcompat",
          "https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/appcompat/",
          "AndroidX compatibility library for older versions of Android",
      )

      OssLibraries.add(
          "AndroidX Activity Compose",
          "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/activity/activity-compose",
          "Jetpack Compose bridge for AndroidX Activity",
      )

      OssLibraries.add(
          "QRCode-Kotlin",
          "https://github.com/g0dkar/qrcode-kotlin#installation",
          "QRCode Generator implemented in pure Kotlin",
          license = OssLicenses.MIT,
      )
    }
  }
}
