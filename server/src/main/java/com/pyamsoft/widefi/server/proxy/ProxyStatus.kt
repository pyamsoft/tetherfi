package com.pyamsoft.widefi.server.proxy

import com.pyamsoft.widefi.server.status.BaseStatusBroadcaster
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class ProxyStatus @Inject internal constructor() : BaseStatusBroadcaster()
