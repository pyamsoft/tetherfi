package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizerImpl
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.event.OnShutdownEvent
import com.pyamsoft.tetherfi.server.logging.ApplicationLogStorage
import com.pyamsoft.tetherfi.server.logging.LogStorage
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.permission.PermissionGuardImpl
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.WifiSharedProxy
import com.pyamsoft.tetherfi.server.proxy.connector.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.connector.factory.DefaultProxyManagerFactory
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.factory.TcpProxySessionFactory
import com.pyamsoft.tetherfi.server.proxy.session.factory.UdpProxySessionFactory
import com.pyamsoft.tetherfi.server.proxy.session.mempool.ByteArrayMemPool
import com.pyamsoft.tetherfi.server.proxy.session.mempool.MemPool
import com.pyamsoft.tetherfi.server.proxy.session.options.TcpProxyOptions
import com.pyamsoft.tetherfi.server.proxy.session.options.UdpProxyOptions
import com.pyamsoft.tetherfi.server.proxy.session.urlfixer.PSNUrlFixer
import com.pyamsoft.tetherfi.server.proxy.session.urlfixer.UrlFixer
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WifiDirectWiDiNetwork
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.tetherfi.server.widi.receiver.WifiDirectReceiver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import java.util.concurrent.Executors
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

@Module
abstract class ServerModule {

  @Binds
  @CheckResult
  internal abstract fun bindShutdownConsumer(
      impl: EventBus<OnShutdownEvent>
  ): EventConsumer<OnShutdownEvent>

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxy(impl: WifiSharedProxy): SharedProxy

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindByteArrayMemPool(impl: ByteArrayMemPool): MemPool<ByteArray>

  @Binds
  @CheckResult
  internal abstract fun bindPermissionChecker(impl: PermissionGuardImpl): PermissionGuard

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetwork(impl: WifiDirectWiDiNetwork): WiDiNetwork

  @Binds
  @CheckResult
  internal abstract fun bindBatteryOptimizer(impl: BatteryOptimizerImpl): BatteryOptimizer

  @Binds
  @IntoSet
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindPSNUrlFixer(impl: PSNUrlFixer): UrlFixer

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindWidiReceiver(impl: WifiDirectReceiver): WiDiReceiver

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxyManagerFactory(
      impl: DefaultProxyManagerFactory
  ): ProxyManager.Factory

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpProxySessionFactory(
      impl: TcpProxySessionFactory
  ): ProxySession.Factory<TcpProxyOptions>

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindUdpProxySessionFactory(
      impl: UdpProxySessionFactory
  ): ProxySession.Factory<UdpProxyOptions>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideErrorBus(): EventBus<ErrorEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideWidiReceiverEventBus(): EventBus<WidiNetworkEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideShutdownEventBus(): EventBus<OnShutdownEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideConnectionBus(): EventBus<ConnectionEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @ServerInternalApi
    internal fun provideProxyDebug(): Boolean {
      return false
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideActivityLogStorage(): LogStorage<ConnectionEvent> {
      return ApplicationLogStorage()
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideErrorLogStorage(): LogStorage<ErrorEvent> {
      return ApplicationLogStorage()
    }

    @Provides
    @JvmStatic
    @Singleton
    @ServerInternalApi
    internal fun provideProxyDispatcher(): CoroutineDispatcher {
      // This is a completely unbounded threadpool that is different from Dispatcher IO
      // so we don't block that dispatcher with network related work
      //
      // We use an unbounded instead of a bounded pool because a bound queue with task waiting is
      // just too slow for network related performance
      //
      // Since most networking is short lived, this pool is suited for quick threading tasks
      return Executors.newCachedThreadPool().asCoroutineDispatcher()
    }
  }
}
