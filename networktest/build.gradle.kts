plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.android")
  id("org.gradle.android.cache-fix")
}

android {
  namespace = "com.pyamsoft.networktest"
  compileSdk = rootProject.extra["compileSdk"] as Int

  defaultConfig {
    applicationId = "com.pyamsoft.networktest"

    minSdk = rootProject.extra["minSdk"] as Int
    targetSdk = rootProject.extra["targetSdk"] as Int

    versionCode = 1
    versionName = "1.0"

    vectorDrawables.useSupportLibrary = true
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Flag to enable support for the new language APIs
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions { jvmTarget = JavaVersion.VERSION_17.majorVersion }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${rootProject.extra["desugar"]}")

  implementation("androidx.activity:activity-compose:${rootProject.extra["composeActivity"]}")
  implementation("androidx.compose.ui:ui:${rootProject.extra["compose"]}")
  implementation("androidx.compose.material3:material3:${rootProject.extra["composeMaterial3"]}")
}
