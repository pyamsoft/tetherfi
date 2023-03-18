package com.pyamsoft.tetherfi.server.proxy.session

import com.pyamsoft.tetherfi.server.ProxyDebug
import com.pyamsoft.tetherfi.server.proxy.ProxyLogger
import com.pyamsoft.tetherfi.server.proxy.SharedProxy

internal abstract class BaseProxySession<T : ProxyData>
protected constructor(
    proxyType: SharedProxy.Type,
    proxyDebug: ProxyDebug,
) :
    ProxySession<T>,
    ProxyLogger(
        proxyType,
        proxyDebug,
    )
