package com.pyamsoft.tetherfi.server.proxy

import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.status.BaseStatusBroadcaster
import javax.inject.Inject

@ServerInternalApi
internal class ProxyStatus @Inject internal constructor() : BaseStatusBroadcaster()
