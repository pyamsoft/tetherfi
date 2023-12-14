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

package com.pyamsoft.tetherfi.status

import android.app.Activity
import android.app.Application
import android.app.Service
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber

object StatusObjectGraph {

  private val trackingMap = mutableMapOf<Application, PartialStatusAppComponent>()

  fun install(
      application: Application,
      component: PartialStatusAppComponent,
  ) {
    trackingMap[application] = component
    Timber.d { "Track ApplicationScoped install: $application $component" }
  }

  @CheckResult
  fun retrieve(activity: Activity): PartialStatusAppComponent {
    return retrieve(activity.application)
  }

  @CheckResult
  fun retrieve(service: Service): PartialStatusAppComponent {
    return retrieve(service.application)
  }

  @CheckResult
  fun retrieve(application: Application): PartialStatusAppComponent {
    return trackingMap[application].requireNotNull {
      "Could not find ApplicationScoped internals for Application: $application"
    }
  }
}
