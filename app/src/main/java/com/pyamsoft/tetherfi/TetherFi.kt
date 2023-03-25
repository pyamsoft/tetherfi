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
            theme = TetherFiThemeProvider,
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
      // We are using pydroid-notify
      OssLibraries.usingNotify = true

      // We are using pydroid-autopsy
      OssLibraries.usingAutopsy = true

      OssLibraries.add(
          "Dagger",
          "https://github.com/google/dagger",
          "A fast dependency injector for Android and Java.",
      )

      OssLibraries.add(
          "Ktor",
          "https://github.com/ktorio/ktor",
          "Framework for quickly creating connected applications in Kotlin with minimal effort",
      )

      OssLibraries.add(
          "Accompanist System UI Controller",
          "https://google.github.io/accompanist/systemuicontroller/",
          "System UI Controller provides easy-to-use utilities for updating the System UI bar colors within Jetpack Compose.",
      )

      OssLibraries.add(
          "Accompanist Pager Indicators",
          "https://google.github.io/accompanist/pager/",
          "A library which provides paging layouts for Jetpack Compose.",
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
