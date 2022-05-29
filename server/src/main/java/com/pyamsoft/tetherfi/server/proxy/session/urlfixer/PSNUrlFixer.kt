package com.pyamsoft.tetherfi.server.proxy.session.urlfixer

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class PSNUrlFixer @Inject internal constructor() : UrlFixer {

  /**
   * PS4 network test, sends initial is-network-connected check URL as
   *
   * GET
   * http://ps4-system.sec.np.dl.playstation.nethttp://ps4-system.sec.np.dl.playstation.net/ps4-system/party/np/v00/party_config.env
   * HTTP/1.1
   *
   * which we know is wrong and should be fixed to
   *
   * GET http://ps4-system.sec.np.dl.playstation.net/ps4-system/party/np/v00/party_config.env
   * HTTP/1.1
   */
  override fun fix(url: String): String {
    if (PSN_REGEX.matches(url)) {
      // Get the second http
      val httpIndex = url.lastIndexOf("http")
      if (httpIndex >= 0) {
        val fixed = url.substring(httpIndex + 1)
        Timber.d("Fixed bad Playstation URL: $url => $fixed")
        return fixed
      }
    }

    return url
  }

  companion object {

    private val PSN_REGEX =
        "^http://\\S*[.]playstation[.]nethttp://\\S*[.]playstation[.]net/S*".toRegex()
  }
}
