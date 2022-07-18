package com.pyamsoft.tetherfi.server.proxy.session

interface ProxyData<R : Any, E : Any> {

  /** The runtime is unique per exchange() in a session */
  val runtime: R

  /** The environment is shared over all exchange() calls in a session */
  val environment: E
}
