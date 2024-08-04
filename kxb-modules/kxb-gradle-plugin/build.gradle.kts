plugins {
  id("kxb.build.conventions.kotlin-gradle-plugin")
  alias(libs.plugins.gradle.pluginPublish)
  `java-test-fixtures`
//    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
  kotlin("plugin.serialization") version embeddedKotlinVersion
}

pluginBundle {
  website = "https://github.com/Kotlin/kotlinx-benchmark"
  vcsUrl = "https://github.com/Kotlin/kotlinx-benchmark.git"
  tags = listOf("benchmarking", "multiplatform", "kotlin")
}

gradlePlugin {
  plugins {
    register("benchmarkPlugin") {
      id = "org.jetbrains.kotlinx.benchmark"
      implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
      displayName = "Gradle plugin for benchmarking"
      description = "Toolkit for running benchmarks for multiplatform Kotlin code."
    }
    register("benchmarkPluginMu") {
      id = "dev.adamko.kotlinx-benchmark"
      implementationClass = "kotlinx.benchmark.gradle.mu.BenchmarkPlugin"
      displayName = "Gradle plugin for benchmarking"
      description = "Toolkit for running benchmarks for multiplatform Kotlin code."
    }
  }
}

kotlin {
  compilerOptions {
    optIn.addAll(
      "kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi",
    )
  }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
//  implementation(libs.kotlin.reflect)

  implementation(projects.kxbRunnerParameters)

  implementation(libs.kotlin.utilKlibMetadata)
  implementation(libs.kotlin.utilKlib)
  implementation(libs.kotlin.utilIo)

  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.allOpen.gradlePlugin)

  compileOnly(projects.kxbGenerator)
  compileOnly(projects.kxbRunner)

  compileOnly(libs.jmh.core) // TODO remove
}

val generatePluginConstants by tasks.registering {
  description = "Generates constants file used by BenchmarksPlugin"

  val outputDir = temporaryDir
  outputs.dir(outputDir).withPropertyName("outputDir")

  val constantsKtFile = outputDir.resolve("BenchmarksPluginConstants.kt")

  val benchmarkPluginVersion = project.providers.gradleProperty("releaseVersion")
    .orElse(project.version.toString())
  inputs.property("benchmarkPluginVersion", benchmarkPluginVersion)

  val jmhVersion = libs.versions.jmh
  inputs.property("jmhVersion", jmhVersion)

  val minSupportedGradleVersion = libs.versions.minSupportedGradle
  inputs.property("minSupportedGradleVersion", minSupportedGradleVersion)

  doLast {
    constantsKtFile.writeText(
      """
      |package kotlinx.benchmark.gradle.internal
      |
      |internal object BenchmarksPluginConstants {
      |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
      |  const val JMH_DEFAULT_VERSION = "${jmhVersion.get()}"
      |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
      |}
      |
      """.trimMargin()
    )
  }
}

sourceSets {
  main {
    kotlin.srcDir(generatePluginConstants)
  }
}

//apiValidation {
//    nonPublicMarkers += ["kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi"]
//}
