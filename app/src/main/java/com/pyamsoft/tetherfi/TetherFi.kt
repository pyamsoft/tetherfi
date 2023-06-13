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

package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.bootstrap.libraries.OssLicenses
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.ui.installPYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.core.TERMS_CONDITIONS_URL

class TetherFi : Application() {

  @CheckResult
  private fun installPYDroid(): ModuleProvider {
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

  private fun installComponent(moduleProvider: ModuleProvider) {
    val mods = moduleProvider.get()
    val component =
        DaggerTetherFiComponent.factory()
            .create(
                debug = isDebugMode(),
                application = this,
                imageLoader = mods.imageLoader(),
                theming = mods.theming(),
                enforcer = mods.enforcer(),
            )
    ObjectGraph.ApplicationScope.install(this, component)
  }

  override fun onCreate() {
    super.onCreate()
    installLogger()
    val modules = installPYDroid()
    installComponent(modules)

    addLibraries()
  }

  companion object {

    @JvmStatic
    private fun addLibraries() {
      OssLibraries.apply {
        usingNotify = true
        usingAutopsy = true
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
          "Accompanist Pager Indicators",
          "https://google.github.io/accompanist/pager/",
          "A library which provides paging layouts for Jetpack Compose.",
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
