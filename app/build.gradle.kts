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

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.android")
  id("org.gradle.android.cache-fix")
}

android {
  namespace = "com.pyamsoft.tetherfi"

  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig {
    applicationId = "com.pyamsoft.tetherfi"

    versionCode = 56
    versionName = "20250723-1"

    minSdk = rootProject.extra["minSdk"] as Int
    targetSdk = rootProject.extra["targetSdk"] as Int

    vectorDrawables.useSupportLibrary = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

  signingConfigs {
    named("debug") {
      storeFile = file("debug.keystore")
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      storePassword = "android"
    }
    create("release") {
      // For some reason with Gradle 8.5 and AGP 8.2.0 we need to load local.properties manually?
      // https://stackoverflow.com/questions/21999829/how-do-i-read-properties-defined-in-local-properties-in-build-gradle
      //
      // Be sure to close the file after!
      val properties =
          rootProject.file("local.properties").reader().use { r -> Properties().apply { load(r) } }

      storeFile = file(properties.getProperty("BUNDLE_STORE_FILE") ?: "CANNOT BUILD")
      keyAlias = properties.getProperty("BUNDLE_KEY_ALIAS") ?: "CANNOT BUILD"
      keyPassword = properties.getProperty("BUNDLE_KEY_PASSWD") ?: "CANNOT BUILD"
      storePassword = properties.getProperty("BUNDLE_STORE_PASSWD") ?: "CANNOT BUILD"
    }
  }

  // https://developer.android.com/build/build-variants
  flavorDimensions += listOf("store")

  productFlavors {
    create("fdroid") {
      dimension = "store"

      // https://github.com/pyamsoft/tetherfi/issues/307
      dependenciesInfo {
        includeInApk = false
        includeInBundle = false
      }
    }

    create("google") {
      dimension = "store"

      // https://github.com/pyamsoft/tetherfi/issues/307
      dependenciesInfo {
        includeInApk = true
        includeInBundle = true
      }
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      // https://developer.android.com/build/shrink-code#native-crash-support
      // androidx.graphics.path includes native code now?
      ndk { debugSymbolLevel = "FULL" }
    }

    debug {
      signingConfig = signingConfigs.getByName("debug")
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"

      // https://developer.android.com/build/shrink-code#native-crash-support
      // androidx.graphics.path includes native code now?
      ndk { debugSymbolLevel = "FULL" }
    }
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

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

  ksp("com.google.dagger:dagger-compiler:${rootProject.extra["dagger"]}")

  // Leak Canary
  debugImplementation(
      "com.squareup.leakcanary:leakcanary-android:${rootProject.extra["leakCanary"]}")
  implementation("com.squareup.leakcanary:plumber-android:${rootProject.extra["leakCanary"]}")

  // AndroidX
  implementation("androidx.appcompat:appcompat:${rootProject.extra["appCompat"]}")
  implementation("androidx.activity:activity-compose:${rootProject.extra["composeActivity"]}")

  // Needed just for androidx.preference.PreferenceManager
  // Eventually, big G may push for DataStore being a requirement, which will be pain
  // This pulls in all the UI bits too, which is a little lame.
  implementation("androidx.preference:preference:${rootProject.extra["preferences"]}")

  // DataStore
  implementation("androidx.datastore:datastore-preferences:${rootProject.extra["dataStore"]}")

  // PYDroid
  implementation("com.github.pyamsoft.pydroid:notify:${rootProject.extra["pydroid"]}")
  implementation("com.github.pyamsoft.pydroid:ui:${rootProject.extra["pydroid"]}")

  implementation(project(":behavior"))
  implementation(project(":connections"))
  implementation(project(":core"))
  implementation(project(":info"))
  implementation(project(":main"))
  implementation(project(":server"))
  implementation(project(":service"))
  implementation(project(":settings"))
  implementation(project(":status"))
  implementation(project(":tile"))
  implementation(project(":ui"))
}
