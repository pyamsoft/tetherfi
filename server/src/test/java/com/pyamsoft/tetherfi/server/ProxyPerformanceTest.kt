package com.pyamsoft.tetherfi.server

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.tcp.HttpTcpTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.utils.io.writeStringUtf8
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import timber.log.Timber

private const val HOSTNAME = "127.0.0.1"
private const val PORT = 9999

private val REMOTE =
    InetSocketAddress(
        hostname = HOSTNAME,
        port = PORT,
    )

private const val LONG_STRING =
    """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum aliquet velit ante, vel ornare mi volutpat sit amet. Curabitur egestas aliquet commodo. Nullam ornare lectus sit amet faucibus fermentum. Sed varius non quam vel hendrerit. Nunc metus nunc, vestibulum eget justo imperdiet, egestas luctus libero. Suspendisse justo augue, sodales auctor porta id, blandit id neque. Ut feugiat egestas porta. Nullam cursus dolor ac massa dapibus, eget ultricies nibh auctor.
Morbi eget eleifend diam, blandit luctus risus. Pellentesque nunc leo, aliquam at dictum et, pretium ac elit. Curabitur volutpat commodo nisl. Nulla elementum et quam vitae blandit. Praesent est justo, ultricies ut iaculis vel, tristique non arcu. Nam porttitor nulla ultrices dui aliquet, et viverra velit egestas. Mauris accumsan magna erat, vitae suscipit turpis lacinia in. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Suspendisse hendrerit aliquam erat, ac pharetra leo bibendum at. Fusce mi mauris, varius quis odio nec, tincidunt tincidunt ex. Vestibulum lorem leo, luctus vitae urna posuere, maximus bibendum magna. Maecenas et ante ut nunc varius rutrum nec quis lorem. Sed consectetur eget leo quis facilisis. Sed feugiat enim in ante pharetra, vitae elementum leo facilisis.
Mauris condimentum velit eget tempus suscipit. Nullam tincidunt faucibus erat et hendrerit. Maecenas non venenatis mi, vel vulputate magna. Sed in consequat velit, id dignissim lorem. Vestibulum sed lobortis massa. Vivamus vitae lorem purus. In hac habitasse platea dictumst. Mauris posuere sagittis lacus, et pretium felis feugiat eleifend. Quisque fringilla mauris sit amet quam vehicula congue. Integer accumsan dolor ut enim volutpat, tincidunt malesuada tellus congue. Praesent aliquet pharetra dignissim. Sed sed volutpat nisi, dapibus semper erat. Nulla rhoncus ligula ac ornare molestie. In a fermentum nibh. Donec et mi ac nisi consequat dignissim ut id mi.
Curabitur in dapibus orci. Vivamus dolor massa, aliquam at turpis id, suscipit convallis tellus. Cras ipsum tellus, malesuada eget arcu in, congue imperdiet sem. Curabitur laoreet felis ut mattis facilisis. Sed vitae pretium nibh. Praesent et lorem in lectus elementum commodo. Nam sit amet cursus lacus. Mauris nec nulla non risus luctus elementum.
Etiam rhoncus dolor est, vel fringilla mauris maximus non. Pellentesque imperdiet eros nisl. Proin cursus velit et elit congue euismod. Ut vehicula nisi eget convallis aliquet. Aliquam porttitor dolor quis ligula volutpat dictum. Suspendisse varius ligula non libero ullamcorper tristique. Nunc fermentum arcu erat.
In non lorem non elit dapibus consectetur. Pellentesque volutpat eros vitae nibh feugiat condimentum. Curabitur lacinia, lorem quis aliquam feugiat, nisl quam pellentesque purus, gravida faucibus neque tellus nec quam. Cras ut orci eget nulla venenatis dictum. Donec bibendum enim ac arcu aliquet pharetra. Ut ut placerat nisl. Sed vulputate eleifend magna vel tincidunt. Fusce imperdiet facilisis massa nec blandit. Cras bibendum leo ut ante cursus fermentum. Curabitur posuere arcu volutpat quam luctus, vitae sagittis mi rhoncus. Donec volutpat at tortor ut suscipit. Duis vitae enim nunc.
Praesent ullamcorper risus vitae libero ullamcorper, nec fringilla enim gravida. Nulla facilisi. Morbi semper sapien cursus, lobortis quam vitae, imperdiet sapien. Proin metus dui, dapibus at ante id, pretium aliquam felis. Pellentesque congue viverra nulla ac rhoncus. Curabitur sed volutpat ipsum. Donec posuere enim nisl, a fermentum dui efficitur vulputate. Morbi feugiat justo at erat tempor, sed iaculis diam tincidunt. Pellentesque tempus sapien libero, ac cursus sem pretium in. Nam porta non tellus in ultricies. Vivamus finibus libero arcu, vel aliquet augue finibus ac. Vivamus pretium, lectus luctus iaculis vehicula, massa nisl tincidunt augue, in laoreet lectus eros sed ligula. Aliquam quis ex non ante vehicula fermentum id sed felis. Nullam vel porta velit. Nulla rhoncus quam sed leo sagittis, sed eleifend eros scelerisque.
Pellentesque feugiat rutrum interdum. Ut eget quam lorem. Morbi sit amet justo luctus ipsum auctor rhoncus. Nam luctus vel nunc vitae dapibus. Nam sit amet maximus arcu. Aliquam justo tellus, gravida vel mi at, maximus scelerisque nulla. Nam vel faucibus felis. Maecenas vitae laoreet mauris. Phasellus porta ex augue, vel malesuada diam scelerisque sed. In hac habitasse platea dictumst. Nam sed metus ut mauris vestibulum molestie. Maecenas tristique ex ac erat iaculis, vel dictum metus venenatis. Fusce ut tincidunt lectus, et iaculis dolor. Nam porttitor mauris libero, sit amet mattis libero porttitor eu.
Integer fermentum dui eget lectus lobortis, eu aliquam magna semper. Pellentesque ultricies dui mi, vel interdum elit fringilla ut. Vestibulum consequat pulvinar libero id tincidunt. In auctor eleifend semper. Sed aliquam pretium egestas. Duis vitae nisi mattis, porta libero non, viverra mauris. Fusce in diam augue. Morbi molestie consequat sodales. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Phasellus semper ex libero, eu mattis velit auctor in. Proin ullamcorper tincidunt quam, eu volutpat metus convallis vel. Donec fringilla sapien at nibh euismod aliquam. Donec aliquet orci justo. Mauris tincidunt, turpis mattis suscipit efficitur, enim nibh tincidunt est, rutrum pulvinar risus ex at quam. Integer placerat dolor turpis, non bibendum diam vehicula ac. Nullam dui nulla, cursus in magna eget, facilisis pellentesque quam.
Nunc rutrum velit vitae ex tempus tincidunt. Vivamus justo magna, pulvinar id nunc ut, imperdiet interdum arcu. Maecenas vehicula risus purus, sit amet condimentum risus lobortis nec. Nunc neque nisi, volutpat eu massa id, consequat interdum ligula. Donec molestie nec tellus vitae consequat. In vitae tortor ac lacus efficitur lobortis et id erat. Morbi ornare orci sed ante viverra fringilla. Aliquam hendrerit dolor nunc, eu sodales velit hendrerit sed. Aliquam massa arcu, elementum vel velit sit amet, ornare tempus mi. Nunc vitae semper diam. Nam sollicitudin tellus vitae felis gravida gravida. Donec egestas velit magna, vestibulum efficitur nisi dictum vel. Vivamus sed varius purus. Donec odio est, pulvinar et sagittis ac, tempor porta magna. Donec urna arcu, porttitor sit amet iaculis sed, lacinia non risus.
"""

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ProxyPerformanceTest {

  private suspend inline fun setupServer(
      scope: CoroutineScope,
      isLoggingEnabled: Boolean = false,
      expectServerFail: Boolean = false,
      appEnv: AppDevEnvironment.() -> Unit = {},
      withServer: (CoroutineDispatcher) -> Unit,
  ) {
    val dispatcher =
        object : ServerDispatcher {
          override val primary = newSingleThreadContext("TEST")
          override val sideEffect = primary

          override fun shutdown() {}
        }

    val enforcer =
        object : ThreadEnforcer {
          override fun assertOffMainThread() {}

          override fun assertOnMainThread() {}
        }

    val blocked =
        object : BlockedClients {
          override fun listenForBlocked(): Flow<Collection<TetherClient>> {
            return flowOf(emptyList())
          }

          override fun isBlocked(client: TetherClient): Boolean {
            return false
          }

          override fun isBlocked(hostNameOrIp: String): Boolean {
            return false
          }
        }

    val allowed =
        object : AllowedClients {
          override fun listenForClients(): Flow<List<TetherClient>> {
            return flowOf(emptyList())
          }

          override suspend fun seen(hostNameOrIp: String) {}

          override suspend fun reportTransfer(hostNameOrIp: String, report: ByteTransferReport) {}
        }

    val socketTagger =
        object : SocketTagger {
          override fun tagSocket() {}
        }

    if (isLoggingEnabled) {
      Timber.plant(
          object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
              t?.printStackTrace()
              println(message)
            }
          })
    }

    val manager =
        TcpProxyManager(
            appEnvironment = AppDevEnvironment().apply(appEnv),
            enforcer = enforcer,
            session =
                TcpProxySession(
                    transports =
                        mutableSetOf(
                            HttpTcpTransport(
                                urlFixers = mutableSetOf(),
                                enforcer = enforcer,
                            ),
                        ),
                    blockedClients = blocked,
                    allowedClients = allowed,
                    enforcer = enforcer,
                    socketTagger = socketTagger,
                ),
            hostConnection =
                BroadcastNetworkStatus.ConnectionInfo.Connected(
                    hostName = HOSTNAME,
                ),
            port = PORT,
            serverDispatcher = dispatcher,
            socketTagger = socketTagger,
            yoloRepeatDelay = 0.seconds,
        )

    val server =
        scope.async {
          val block = suspend { manager.loop {} }

          if (expectServerFail) {
            assertFailsWith<IOException> { block() }
          } else {
            block()
          }
        }

    delay(1.seconds)

    withServer(dispatcher.primary)

    server.cancel()
  }

  /**
   * This is like runTest, but it does not skip delay() calls.
   *
   * We need to actually be able to delay, since server spinup takes a "little bit" of time.
   */
  private fun runBlockingWithDelays(
      timeout: Duration = 10.seconds,
      block: suspend CoroutineScope.() -> Unit,
  ): Unit = runBlocking { withTimeout(timeout, block) }

  /** We can open a bunch of sockets right? */
  @Test
  fun serverPerformanceTest(): Unit = runBlockingWithDelays {
    setupServer(this) { dispatcher ->
      for (i in 0 until 25_000) {
        usingSocketBuilder(dispatcher) { builder ->
          builder
              .tcp()
              .configure {
                reuseAddress = true
                reusePort = true
              }
              .connect(remoteAddress = REMOTE)
              .usingConnection(autoFlush = true) { _, write ->
                write.writeStringUtf8(LONG_STRING)
                write.flush()
              }
        }
      }
    }
  }

  /** We also are prepared to handle when a socket fails to open right? */
  @Test
  fun yoloFailThrows(): Unit = runBlockingWithDelays {
    setupServer(
        this,
        expectServerFail = true,
        appEnv = { updateYolo(true) },
    ) {
      delay(5.seconds)
    }
  }
}
