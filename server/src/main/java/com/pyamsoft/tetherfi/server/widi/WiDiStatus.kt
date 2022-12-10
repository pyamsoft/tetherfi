package com.pyamsoft.tetherfi.server.widi

import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.status.BaseStatusBroadcaster
import javax.inject.Inject

@ServerInternalApi
internal class WiDiStatus @Inject internal constructor() : BaseStatusBroadcaster()
