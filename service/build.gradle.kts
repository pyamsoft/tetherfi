/*
 * Copyright 2025 pyamsoft
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.ksp)
  alias(libs.plugins.android)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.android.cacheFix)
}

android {
  namespace = "com.pyamsoft.tetherfi.service"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }

  buildFeatures { buildConfig = false }
}

dependencies {
  coreLibraryDesugaring(libs.android.desugar)

  ksp(libs.dagger.compiler)

  // API for Dagger to inject
  // Notifier in ServiceAppModule
  // NotifyDispatcher in ServiceAppModule
  api(libs.pydroid.notify)
  implementation(libs.pydroid.bus)

  implementation(project(":core"))
  implementation(project(":server"))
}
