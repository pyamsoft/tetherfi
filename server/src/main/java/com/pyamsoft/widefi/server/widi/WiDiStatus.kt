package com.pyamsoft.widefi.server.widi

import com.pyamsoft.widefi.server.ServerInternalApi
import com.pyamsoft.widefi.server.status.BaseStatusBroadcaster
import javax.inject.Inject

@ServerInternalApi
internal class WiDiStatus @Inject internal constructor() : BaseStatusBroadcaster()
