import java.time.Year

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "ca.justinmo.countdown"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "ca.justinmo.countdown"
    minSdk = 26
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"

    // Year stamped into the copyright notice, generated at build time so it stays current.
    buildConfigField("int", "COPYRIGHT_YEAR", "${Year.now().value}")

    javaCompileOptions { annotationProcessorOptions { arguments += "room.incremental" to "true" } }
  }

  buildTypes {
    getByName("debug") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      testProguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguardTest-rules.pro",
      )
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }

    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      testProguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguardTest-rules.pro",
      )
    }
  }

  // Always show the result of every unit test, even if it passes.
  testOptions.unitTests {
    isIncludeAndroidResources = true

    all { test ->
      with(test) {
        testLogging {
          events =
              setOf(
                  org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                  org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                  org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                  org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
                  org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
              )
        }
      }
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging { resources { excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") } }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
      freeCompilerArgs.add("-opt-in=kotlin.Experimental")
    }
  }
}

/*
Dependency versions are defined in the top level build.gradle file. This helps keeping track of
all versions in a single place. This improves readability and helps managing project complexity.
*/
dependencies {

  // App dependencies
  implementation(libs.androidx.annotation)
  implementation(libs.kotlinx.coroutines.android)

  // Tests
  testImplementation(libs.junit)

  // Architecture Components
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  debugImplementation(libs.androidx.ui.tooling)
  ksp(libs.room.compiler)
  implementation(libs.androidx.lifecycle.runtimeCompose)
  implementation(libs.androidx.lifecycle.viewModelCompose)

  // Hilt
  implementation(libs.hilt.android.core)
  implementation(libs.androidx.hilt.navigation.compose)
  ksp(libs.hilt.compiler)

  // Jetpack Compose
  val composeBom = platform(libs.androidx.compose.bom)

  implementation(libs.androidx.activity.compose)
  implementation(composeBom)
  implementation(libs.androidx.compose.foundation.core)
  implementation(libs.androidx.compose.foundation.layout)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.runtimeCompose)
  implementation(libs.androidx.lifecycle.viewModelCompose)
  implementation(libs.accompanist.appcompat.theme)
  implementation(libs.accompanist.swiperefresh)
  implementation(libs.material)
}
