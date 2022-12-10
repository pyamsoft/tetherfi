package com.pyamsoft.tetherfi.server.proxy

import android.content.Context
import android.service.quicksettings.TileService
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.status.BaseStatusBroadcaster
import javax.inject.Inject

@ServerInternalApi
internal class ProxyStatus
@Inject
internal constructor(
    context: Context,
    tileServiceClass: Class<out TileService>,
) : BaseStatusBroadcaster(context, tileServiceClass)
