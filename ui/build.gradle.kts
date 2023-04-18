/*
 * Copyright 2021 Peter Kenji Yamanaka
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

plugins { id("com.android.library") }

android {
  namespace = "com.pyamsoft.tetherfi.ui"

  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig {
    minSdk = rootProject.extra["minSdk"] as Int

    resourceConfigurations += setOf("en")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions { jvmTarget = "17" }

  buildFeatures {
    buildConfig = false
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "${rootProject.extra["compose_compiler_version"]}"
  }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

  kapt("com.google.dagger:dagger-compiler:${rootProject.extra["dagger"]}")

  // Compose
  api("androidx.compose.compiler:compiler:${rootProject.extra["compose_compiler_version"]}")
  api("androidx.activity:activity-compose:${rootProject.extra["composeActivity"]}")
  api("androidx.compose.ui:ui:${rootProject.extra["compose_version"]}")
  api("androidx.compose.material:material:${rootProject.extra["composeMaterial"]}")
  api("androidx.compose.animation:animation:${rootProject.extra["compose_version"]}")
  api("androidx.compose.ui:ui-tooling-preview:${rootProject.extra["compose_version"]}")
  debugApi("androidx.compose.ui:ui-tooling:${rootProject.extra["compose_version"]}")
  //  api("androidx.compose.material:material-icons-extended:1.3.1")

  api("com.github.pyamsoft.pydroid:bootstrap:${rootProject.extra["pydroid"]}")

  api("io.coil-kt:coil-compose-base:${rootProject.extra["coil"]}")

  api("com.google.accompanist:accompanist-systemuicontroller:${rootProject.extra["accompanist"]}")
  api("com.google.accompanist:accompanist-pager-indicators:${rootProject.extra["accompanist"]}")

  // Material Design
  api("com.google.android.material:material:${rootProject.extra["materialDesign"]}")

  implementation("io.github.g0dkar:qrcode-kotlin-android:${rootProject.extra["qrCode"]}")

  api(project(":core"))
  api(project(":server"))
}
