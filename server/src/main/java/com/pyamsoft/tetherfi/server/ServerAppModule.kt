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

package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.bus.internal.DefaultEventBus
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizerImpl
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClientTracker
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ClientEditor
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.clients.ClientManagerImpl
import com.pyamsoft.tetherfi.server.clients.ClientResolver
import com.pyamsoft.tetherfi.server.clients.StartedClients
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.network.AndroidNetworkBinder
import com.pyamsoft.tetherfi.server.network.NetworkBinder
import com.pyamsoft.tetherfi.server.prereq.location.AndroidLocationChecker
import com.pyamsoft.tetherfi.server.prereq.location.LocationChecker
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuardImpl
import com.pyamsoft.tetherfi.server.prereq.vpn.AndroidVpnChecker
import com.pyamsoft.tetherfi.server.prereq.vpn.VpnChecker
import com.pyamsoft.tetherfi.server.proxy.AndroidSocketTagger
import com.pyamsoft.tetherfi.server.proxy.DefaultServerDispatcherFactory
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.WifiSharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.factory.DefaultProxyManagerFactory
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.HttpTcpTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.RequestParser
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpSessionTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.UrlRequestParser
import com.pyamsoft.tetherfi.server.urlfixer.PSNUrlFixer
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
abstract class ServerAppModule {

  @Binds
  @CheckResult
  internal abstract fun bindSocketTagger(impl: AndroidSocketTagger): SocketTagger

  @Binds @CheckResult internal abstract fun bindRequestParser(impl: UrlRequestParser): RequestParser

  @Binds
  @CheckResult
  internal abstract fun bindShutdownConsumer(
      impl: EventBus<ServerShutdownEvent>
  ): EventConsumer<ServerShutdownEvent>

  @Binds
  @CheckResult
  internal abstract fun bindStopRequestConsumer(
      impl: EventBus<ServerStopRequestEvent>
  ): EventConsumer<ServerStopRequestEvent>

  // Prereqs
  @Binds
  @CheckResult
  internal abstract fun bindPermissionChecker(impl: PermissionGuardImpl): PermissionGuard

  @Binds @CheckResult internal abstract fun bindVpnChecker(impl: AndroidVpnChecker): VpnChecker

  @Binds
  @CheckResult
  internal abstract fun bindLocationChecker(impl: AndroidLocationChecker): LocationChecker

  // OS level
  @Binds
  @CheckResult
  internal abstract fun bindBatteryOptimizer(impl: BatteryOptimizerImpl): BatteryOptimizer

  // Client tracking
  @Binds
  @CheckResult
  internal abstract fun bindBlockedClients(impl: ClientManagerImpl): BlockedClients

  @Binds @CheckResult internal abstract fun bindSeenClients(impl: ClientManagerImpl): AllowedClients

  @Binds @CheckResult internal abstract fun bindClientEraser(impl: ClientManagerImpl): ClientEraser

  @Binds @CheckResult internal abstract fun bindClientEditor(impl: ClientManagerImpl): ClientEditor

  @Binds
  @CheckResult
  internal abstract fun bindClientResolver(impl: ClientManagerImpl): ClientResolver

  @Binds
  @CheckResult
  internal abstract fun bindStartedClients(impl: ClientManagerImpl): StartedClients

  @Binds
  @CheckResult
  internal abstract fun bindBlockedClientTracker(impl: ClientManagerImpl): BlockedClientTracker

  // URL fixers
  @Binds
  @IntoSet
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindPSNUrlFixer(impl: PSNUrlFixer): UrlFixer

  // Proxy
  @Binds @CheckResult internal abstract fun bindProxy(impl: WifiSharedProxy): SharedProxy

  // Network Binder
  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindNetworkBinder(impl: AndroidNetworkBinder): NetworkBinder

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindServerDispatcherFactory(
      impl: DefaultServerDispatcherFactory
  ): ServerDispatcher.Factory

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindProxyManagerFactory(
      impl: DefaultProxyManagerFactory
  ): ProxyManager.Factory

  // TCP
  @Binds
  @IntoSet
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpHttpTransport(impl: HttpTcpTransport): TcpSessionTransport

  @Binds
  @CheckResult
  @ServerInternalApi
  internal abstract fun bindTcpProxySession(impl: TcpProxySession): ProxySession<TcpProxyData>

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideShutdownEventBus(): EventBus<ServerShutdownEvent> {
      return DefaultEventBus()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideStopRequestEventBus(): EventBus<ServerStopRequestEvent> {
      return DefaultEventBus()
    }
  }
}
