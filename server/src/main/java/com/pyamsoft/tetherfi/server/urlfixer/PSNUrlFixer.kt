/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.urlfixer

import com.pyamsoft.tetherfi.core.Timber
import javax.inject.Inject

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
        Timber.d { "Fixed bad Playstation URL: $url => $fixed" }
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
