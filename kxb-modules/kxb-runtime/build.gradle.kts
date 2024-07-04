import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
  id("kxb.build.conventions.kotlin-multiplatform")
  id("kxb.build.conventions.publishing")
}

kotlin {
  // Tier 1
  linuxX64()
  macosX64()
  macosArm64()
  iosSimulatorArm64()
  iosX64()

  // Tier 2
  linuxArm64()
  watchosSimulatorArm64()
  watchosX64()
  watchosArm32()
  watchosArm64()
  tvosSimulatorArm64()
  tvosX64()
  tvosArm64()
  iosArm64()

  // Tier 3
  androidNativeArm32()
  androidNativeArm64()
  androidNativeX86()
  androidNativeX64()
  mingwX64()
  watchosDeviceArm64()

  jvm()
  js(IR) { nodejs() }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs { d8() }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("jsWasmJsShared") {
        withJs()
        withWasm()
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compilerOptions.configure {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll(
          "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi",
          "kotlin.experimental.ExperimentalNativeApi",
          "kotlin.native.runtime.NativeRuntimeApi",
          "kotlinx.cinterop.ExperimentalForeignApi",
        )
      }
    }
  }

  sourceSets {
    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    jvmMain {
      dependencies {
        compileOnly(libs.jmh.core)
      }
    }
    jvmTest {
      dependencies {
        implementation(libs.jmh.core)
      }
    }
    jsMain {
      //jsIrMain.dependsOn(it)
    }
  }
}
