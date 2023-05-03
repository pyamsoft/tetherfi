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

plugins { id("com.android.application") }

android {
  namespace = "com.pyamsoft.tetherfi"

  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig {
    applicationId = "com.pyamsoft.tetherfi"

    versionCode = 23
    versionName = "20230430-1"

    minSdk = rootProject.extra["minSdk"] as Int
    targetSdk = rootProject.extra["targetSdk"] as Int

    resourceConfigurations += setOf("en")

    vectorDrawables.useSupportLibrary = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions { jvmTarget = "17" }

  signingConfigs {
    getByName("debug") {
      storeFile = file("debug.keystore")
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      storePassword = "android"
    }
    create("release") {
      storeFile = file(project.findProperty("BUNDLE_STORE_FILE")?.toString() ?: "CANNOT BUILD")
      keyAlias = project.findProperty("BUNDLE_KEY_ALIAS")?.toString() ?: "CANNOT BUILD"
      keyPassword = project.findProperty("BUNDLE_KEY_PASSWD")?.toString() ?: "CANNOT BUILD"
      storePassword = project.findProperty("BUNDLE_STORE_PASSWD")?.toString() ?: "CANNOT BUILD"
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    debug {
      signingConfig = signingConfigs.getByName("debug")
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
    }
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

  composeOptions { kotlinCompilerExtensionVersion = "${rootProject.extra["composeCompiler"]}" }

  // Fixes this error message
  // More than one file was found with OS independent path "META-INF/core_release.kotlin_module"
  packaging {
    resources.pickFirsts +=
        setOf(
            "META-INF/core_release.kotlin_module",
            "META-INF/ui_release.kotlin_module",
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties",
        )
  }
}

// Leave at bottom
// apply plugin: "com.google.gms.google-services"
dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${rootProject.extra["desugar"]}")

  kapt("com.google.dagger:dagger-compiler:${rootProject.extra["dagger"]}")

  // Leak Canary
  debugImplementation(
      "com.squareup.leakcanary:leakcanary-android:${rootProject.extra["leakCanary"]}")
  implementation("com.squareup.leakcanary:plumber-android:${rootProject.extra["leakCanary"]}")

  // Autopsy
  debugImplementation("com.github.pyamsoft.pydroid:autopsy:${rootProject.extra["pydroid"]}")

  // AndroidX
  api("androidx.fragment:fragment-ktx:1.5.7")
  api("androidx.appcompat:appcompat:1.6.1")

  api(project(":connections"))
  api(project(":info"))
  api(project(":main"))
  api(project(":server"))
  api(project(":service"))
  api(project(":settings"))
  api(project(":status"))
  api(project(":tile"))
}
