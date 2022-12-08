package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.ui.installPYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.core.TERMS_CONDITIONS_URL
import timber.log.Timber

class TetherFi : Application() {

  private var component: TetherFiComponent? = null

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
            debug =
                PYDroid.DebugParameters(
                    enabled = true,
                    upgradeAvailable = true,
                    ratingAvailable = false,
                ),
        ),
    )
  }

  private fun installComponent(moduleProvider: ModuleProvider) {
    if (component == null) {
      val mods = moduleProvider.get()
      component =
          DaggerTetherFiComponent.factory()
              .create(
                  application = this,
                  debug = isDebugMode(),
                  imageLoader = mods.imageLoader(),
                  theming = mods.theming(),
              )
    } else {
      Timber.w("Cannot install TetherFiComponent again")
    }
  }

  @CheckResult
  private fun componentGraph(): TetherFiComponent {
    return component.requireNotNull { "TetherFiComponent was not installed, something is wrong." }
  }

  override fun onCreate() {
    super.onCreate()
    installLogger()
    val modules = installPYDroid()
    installComponent(modules)

    addLibraries()
  }

  override fun getSystemService(name: String): Any? {
    return if (name == TetherFiComponent::class.java.name) componentGraph()
    else super.getSystemService(name)
  }

  companion object {

    @JvmStatic
    private fun addLibraries() {
      // We are using pydroid-notify
      OssLibraries.usingNotify = true

      // We are using pydroid-autopsy
      OssLibraries.usingAutopsy = true

      // We are using pydroid-inject
      OssLibraries.usingInject = true

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
    }
  }
}
