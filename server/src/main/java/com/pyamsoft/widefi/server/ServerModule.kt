package com.pyamsoft.widefi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.widefi.server.permission.PermissionGuard
import com.pyamsoft.widefi.server.permission.PermissionGuardImpl
import com.pyamsoft.widefi.server.proxy.ProxyStatus
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.WifiSharedProxy
import com.pyamsoft.widefi.server.status.StatusListener
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import com.pyamsoft.widefi.server.widi.WiDiStatus
import com.pyamsoft.widefi.server.widi.WifiDirectWiDiNetwork
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class ServerModule {

  @Binds @CheckResult internal abstract fun bindProxy(impl: WifiSharedProxy): SharedProxy

  @Binds
  @CheckResult
  internal abstract fun bindPermissionChecker(impl: PermissionGuardImpl): PermissionGuard

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetwork(impl: WifiDirectWiDiNetwork): WiDiNetwork

  @Binds @CheckResult internal abstract fun bindWiDiStatusListener(impl: WiDiStatus): StatusListener

  @Binds
  @CheckResult
  internal abstract fun bindProxyStatusListener(impl: ProxyStatus): StatusListener

  @Binds
  @CheckResult
  internal abstract fun bindErrorListener(
      impl: EventBus<ErrorEvent>
  ): EventConsumer<ErrorEvent>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideErrorBus(): EventBus<ErrorEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @Singleton
    @Named("proxy_debug")
    internal fun provideProxyDebug(): Boolean {
      return true
    }
  }
}
