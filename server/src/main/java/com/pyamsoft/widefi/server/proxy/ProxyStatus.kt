package com.pyamsoft.widefi.server.proxy

import com.pyamsoft.widefi.server.ServerInternalApi
import com.pyamsoft.widefi.server.status.BaseStatusBroadcaster
import javax.inject.Inject

@ServerInternalApi
internal class ProxyStatus @Inject internal constructor() : BaseStatusBroadcaster()
