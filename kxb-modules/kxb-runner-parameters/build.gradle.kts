import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
  id("kxb.build.conventions.kotlin-multiplatform")
  id("kxb.build.conventions.publishing")
  kotlin("plugin.serialization") version "2.0.0"
}

kotlin {
  linuxX64()
  macosX64()
  macosArm64()
  mingwX64()

  linuxArm64()

  iosArm64()
  iosSimulatorArm64()
  iosX64()
  tvosArm64()
  tvosSimulatorArm64()
  tvosX64()
  watchosArm32()
  watchosArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  watchosX64()

  androidNativeArm32()
  androidNativeArm64()
  androidNativeX86()
  androidNativeX64()

  jvm()

  js(IR) { nodejs() }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs { d8() }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("nonJvm") {
        group("jsHosted") {
          withJs()
          withWasmJs()
        }
      }
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    allWarningsAsErrors = false
//    freeCompilerArgs.add("-Xexpect-actual-classes")
//    optIn.addAll(
//      "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi",
//      "kotlin.experimental.ExperimentalNativeApi",
//      "kotlin.native.runtime.NativeRuntimeApi",
//      "kotlinx.cinterop.ExperimentalForeignApi",
//    )
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
//    jvmMain {
//      dependencies {
//        compileOnly(libs.jmh.core)
//      }
//    }
//    jvmTest {
//      dependencies {
//        implementation(libs.jmh.core)
//      }
//    }
  }
}
