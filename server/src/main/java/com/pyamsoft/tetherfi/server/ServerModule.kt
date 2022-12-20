package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizerImpl
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.permission.PermissionGuardImpl
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.WifiSharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.factory.DefaultProxyManagerFactory
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ByteArrayMemPool
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ManagedMemPool
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.ManagedKeyedObjectProducer
import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.UdpKeyedObjectProducer
import com.pyamsoft.tetherfi.server.urlfixer.PSNUrlFixer
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import com.pyamsoft.tetherfi.server.widi.WiDiConfig
import com.pyamsoft.tetherfi.server.widi.WiDiConfigImpl
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkImpl
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiverRegister
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.tetherfi.server.widi.receiver.WifiDirectReceiver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.ktor.network.sockets.ConnectedDatagramSocket
import java.util.concurrent.Executors
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

@Module
abstract class ServerModule {

  @Binds
  @CheckResult
  internal abstract fun bindShutdownConsumer(
      impl: EventBus<ServerShutdownEvent>
  ): EventConsumer<ServerShutdownEvent>

  @Binds
  @CheckResult
  internal abstract fun bindPermissionChecker(impl: PermissionGuardImpl): PermissionGuard

  @Binds @CheckResult internal abstract fun bindWiDiNetwork(impl: WiDiNetworkImpl): WiDiNetwork

  @Binds
  @CheckResult
  internal abstract fun bindWiDiNetworkStatus(impl: WiDiNetworkImpl): WiDiNetworkStatus

  @Binds
  @CheckResult
  internal abstract fun bindBatteryOptimizer(impl: BatteryOptimizerImpl): BatteryOptimizer

  // Internals

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxy(impl: WifiSharedProxy): SharedProxy

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindConfig(impl: WiDiConfigImpl): WiDiConfig

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpMemPool(impl: ByteArrayMemPool): ManagedMemPool<ByteArray>

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindUdpProducer(
      impl: UdpKeyedObjectProducer
  ): ManagedKeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket>

  @Binds
  @IntoSet
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindPSNUrlFixer(impl: PSNUrlFixer): UrlFixer

  @Binds @CheckResult internal abstract fun bindWidiReceiver(impl: WifiDirectReceiver): WiDiReceiver

  @Binds
  @CheckResult
  internal abstract fun bindWidiReceiverRegister(impl: WifiDirectReceiver): WiDiReceiverRegister

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxyManagerFactory(
      impl: DefaultProxyManagerFactory
  ): ProxyManager.Factory

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpProxySession(impl: TcpProxySession): ProxySession<TcpProxyData>

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindUdpProxySession(impl: UdpProxySession): ProxySession<UdpProxyData>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    @Named("server")
    internal fun provideServerPermissionRequester(guard: PermissionGuard): PermissionRequester {
      return PermissionRequester.create(guard.requiredPermissions.toTypedArray())
    }

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
    internal fun provideShutdownEventBus(): EventBus<ServerShutdownEvent> {
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
