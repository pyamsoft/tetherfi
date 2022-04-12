package com.pyamsoft.widefi.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.app.PYDroidActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.widefi.R
import com.pyamsoft.widefi.WidefiComponent
import com.pyamsoft.widefi.service.ProxyService
import javax.inject.Inject

class MainActivity : PYDroidActivity() {

  @Inject @JvmField internal var viewModel: MainViewModeler? = null

  override val applicationIcon = R.mipmap.ic_launcher_round

  override val changelog = buildChangeLog {}

  private fun handleToggleProxy() {
    viewModel
        .requireNotNull()
        .handleToggleProxy(
            scope = lifecycleScope,
            onStart = { ProxyService.start(this) },
            onStop = { ProxyService.stop(this) },
        )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Injector.obtainFromApplication<WidefiComponent>(this).inject(this)

    val vm = viewModel.requireNotNull()

    setContent {
      vm.Render { state ->
        MainScreen(
            state = state,
            onToggle = { handleToggleProxy() },
            onTabUpdated = { vm.handleUpdateView(it) },
        )
      }
    }

    vm.watchStatusUpdates(scope = lifecycleScope)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel = null
  }
}
