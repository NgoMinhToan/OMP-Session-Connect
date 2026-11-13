import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// Version comes from gradle.properties locally and from -P flags in CI, where the
// workflow derives it from the commit count so every published build is unique.
val appVersionName: String = providers.gradleProperty("omp.versionName").getOrElse("1.0.0")
val appVersionCode: Int = providers.gradleProperty("omp.versionCode").getOrElse("1").toInt()

// Release signing. The keystore is intentionally committed so CI can produce
// installable APKs without extra secrets; see docs/RELEASING.md.
val keystoreDir = rootProject.layout.projectDirectory.dir("keystore")
val keystoreFile = keystoreDir.file("omp-release.jks").asFile
val keystoreProperties = Properties().apply {
  val propsFile = keystoreDir.file("keystore.properties").asFile
  if (propsFile.exists()) propsFile.inputStream().use(::load)
}

fun signingValue(key: String, env: String, fallback: String): String =
  keystoreProperties.getProperty(key) ?: System.getenv(env) ?: fallback

android {
  namespace = "com.ompsessionconnect"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.ompsessionconnect"
    minSdk = 26
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName
  }

  signingConfigs {
    create("release") {
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = signingValue("storePassword", "OMP_KEYSTORE_PASSWORD", "omp-session-connect")
        keyAlias = signingValue("keyAlias", "OMP_KEY_ALIAS", "omp")
        keyPassword = signingValue("keyPassword", "OMP_KEY_PASSWORD", "omp-session-connect")
        enableV1Signing = true
        enableV2Signing = true
      }
    }
  }

  // One APK per ABI plus a universal APK that runs anywhere.
  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
    }
    // Same code and signing, R8 and resource shrinking disabled. Useful for
    // readable stack traces and for comparing sizes against `release`.
    create("releaseNonMinify") {
      initWith(getByName("release"))
      isMinifyEnabled = false
      isShrinkResources = false
      matchingFallbacks += listOf("release")
      if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":domain"))
  implementation(project(":data"))

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.haze)
  implementation(libs.haze.blur)
  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
