/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.PYDroidLogger
import com.pyamsoft.pydroid.ui.debug.InAppDebugLogger.Companion.createInAppDebugLogger
import com.pyamsoft.pydroid.util.isDebugMode
import timber.log.Timber

fun Application.installLogger() {
  val self = this
  if (isDebugMode()) {
    // For debug logcat
    Timber.plant(
        object : Timber.DebugTree() {
          override fun createStackElementTag(element: StackTraceElement): String {
            return element.run { "($fileName:$lineNumber)" }
          }
        },
    )
  }

  // For optional in-app debug
  Timber.plant(
      object : Timber.Tree() {

        private val logger = self.createInAppDebugLogger()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
          logger.log(priority, tag, message, t)
        }
      },
  )
}

@CheckResult
fun createLogger(): PYDroidLogger {
  return object : PYDroidLogger {

    override fun d(
        tag: String,
        message: String,
        vararg args: Any,
    ) {
      Timber.tag(tag).d(message, args)
    }

    override fun w(
        tag: String,
        message: String,
        vararg args: Any,
    ) {
      Timber.tag(tag).w(message, args)
    }

    override fun e(
        tag: String,
        throwable: Throwable,
        message: String,
        vararg args: Any,
    ) {
      Timber.tag(tag).e(throwable, message, args)
    }
  }
}
