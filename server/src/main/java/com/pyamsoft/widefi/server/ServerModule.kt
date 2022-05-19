package com.pyamsoft.widefi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.logging.ApplicationLogStorage
import com.pyamsoft.widefi.server.logging.LogStorage
import com.pyamsoft.widefi.server.permission.PermissionGuard
import com.pyamsoft.widefi.server.permission.PermissionGuardImpl
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.WifiSharedProxy
import com.pyamsoft.widefi.server.proxy.connector.ProxyManager
import com.pyamsoft.widefi.server.proxy.connector.factory.DefaultProxyManagerFactory
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.session.UrlFixer
import com.pyamsoft.widefi.server.proxy.session.factory.TcpProxySessionFactory
import com.pyamsoft.widefi.server.proxy.session.factory.UdpProxySessionFactory
import com.pyamsoft.widefi.server.proxy.session.mempool.ByteArrayMemPool
import com.pyamsoft.widefi.server.proxy.session.mempool.MemPool
import com.pyamsoft.widefi.server.proxy.session.urlfixer.PSNUrlFixer
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import com.pyamsoft.widefi.server.widi.WifiDirectWiDiNetwork
import com.pyamsoft.widefi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.widefi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.widefi.server.widi.receiver.WifiDirectReceiver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.Socket
import java.util.concurrent.Executors
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

@Module
abstract class ServerModule {

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
  ): ProxySession.Factory<Socket>

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindUdpProxySessionFactory(
      impl: UdpProxySessionFactory
  ): ProxySession.Factory<Datagram>

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
