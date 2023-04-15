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

plugins {
  id("com.android.library")
}

//noinspection GroovyMissingReturnStatement
android {
  namespace = "com.pyamsoft.tetherfi.settings"

  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig {
    minSdk = rootProject.extra["minSdk"] as Int

    resourceConfigurations += setOf("en")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    buildConfig = false
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "${rootProject.extra["compose_compiler_version"]}"
  }
}

dependencies {
  kapt("com.google.dagger:dagger-compiler:${rootProject.extra["dagger"]}")

  api(project(":ui"))
}
