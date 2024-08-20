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

plugins {
  id("com.android.library")
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.compose")
  id("kotlin-android")
  id("org.gradle.android.cache-fix")
}

android {
  namespace = "com.pyamsoft.tetherfi.ui"

  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig { minSdk = rootProject.extra["minSdk"] as Int }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions { jvmTarget = JavaVersion.VERSION_17.majorVersion }

  buildFeatures {
    buildConfig = false
    compose = true
  }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${rootProject.extra["desugar"]}")

  ksp("com.google.dagger:dagger-compiler:${rootProject.extra["dagger"]}")

  // Lifecycle extensions
  api("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

  // Compose
  api("androidx.compose.ui:ui:${rootProject.extra["compose"]}")
  api("androidx.compose.animation:animation:${rootProject.extra["compose"]}")
  api("androidx.compose.material3:material3:${rootProject.extra["composeMaterial3"]}")
  // api("androidx.compose.material:material-icons-extended:${rootProject.extra["composeMaterial"]}")

  // Compose Preview
  api("androidx.compose.ui:ui-tooling-preview:${rootProject.extra["compose"]}")
  debugApi("androidx.compose.ui:ui-tooling:${rootProject.extra["compose"]}")

  // WiFi QR Code
  api("io.coil-kt:coil-compose-base:2.7.0")
  implementation("io.github.g0dkar:qrcode-kotlin-android:4.2.0")

  // PYDroid
  implementation("com.github.pyamsoft.pydroid:ui:${rootProject.extra["pydroid"]}")

  implementation(project(":core"))
  implementation(project(":server"))
}
