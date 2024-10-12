import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  id("kxb.build.conventions.kotlin-multiplatform")
  id("kxb.build.conventions.publishing")
  alias(libs.plugins.kotlinx.serialization)
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
    freeCompilerArgs.add("-Xexpect-actual-classes")
    optIn.addAll(
      "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi",
      "kotlin.experimental.ExperimentalNativeApi",
    )
  }

  targets.withType<KotlinNativeTarget>().configureEach {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      optIn.addAll(
        "kotlin.native.runtime.NativeRuntimeApi",
        "kotlinx.cinterop.ExperimentalForeignApi",
      )
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.kxbRunnerParameters)
        api(libs.kotlinxSerialization.json)
      }
    }
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
    listOf(
      linuxMain,
      nativeMain,
      appleMain,
      iosMain,
      macosMain,
      tvosMain,
      watchosMain,
      androidNativeMain,
    ).forEach {
      it.configure {
        languageSettings {
          optIn("kotlin.native.runtime.NativeRuntimeApi")
          optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
      }
    }
  }
}

rootProject.plugins.withType<YarnPlugin>().configureEach {
  rootProject.extensions.configure<YarnRootExtension> {
    yarnLockMismatchReport = YarnLockMismatchReport.NONE
    yarnLockAutoReplace = true
    reportNewYarnLock = false
    lockFileDirectory = rootDir.resolve(".kotlin/js-store")
  }
}
