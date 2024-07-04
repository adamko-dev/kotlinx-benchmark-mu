import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("kxb.build.conventions.kotlin-gradle-plugin")
    alias(libs.plugins.gradle.pluginPublish)
//    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

pluginBundle {
    website = "https://github.com/Kotlin/kotlinx-benchmark"
    vcsUrl = "https://github.com/Kotlin/kotlinx-benchmark.git"
    tags = listOf("benchmarking", "multiplatform", "kotlin")
}

gradlePlugin {
    plugins {
        create("benchmarkPlugin") {
            id = "org.jetbrains.kotlinx.benchmark"
            implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
            displayName = "Gradle plugin for benchmarking"
            description = "Toolkit for running benchmarks for multiplatform Kotlin code."
        }
    }
}

//tasks.named("compileKotlin", KotlinCompilationTask.class) {
//    compilerOptions {
//        optIn.addAll(
//                "kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi",
//                "kotlin.RequiresOptIn",
//        )
//        //noinspection GrDeprecatedAPIUsage
//        apiVersion = KotlinVersion.KOTLIN_1_4 // the version of Kotlin embedded in Gradle
//    }
//}

dependencies {
    implementation(libs.kotlin.reflect)

    implementation(libs.squareup.kotlinpoet)

    implementation(libs.kotlin.utilKlibMetadata)
    implementation(libs.kotlin.utilKlib)
    implementation(libs.kotlin.utilIo)

    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compilerEmbeddable)
    compileOnly(libs.jmh.generatorBytecode) // used in worker
}

//def generatePluginConstants = tasks.register("generatePluginConstants") {
//    description = "Generates constants file used by BenchmarksPlugin"
//
//    File outputDir = temporaryDir
//    outputs.dir(outputDir).withPropertyName("outputDir")
//
//    File constantsKtFile = new File(outputDir, "BenchmarksPluginConstants.kt")
//
//    Provider<String> benchmarkPluginVersion = project.providers.gradleProperty("releaseVersion")
//            .orElse(project.version.toString())
//    inputs.property("benchmarkPluginVersion", benchmarkPluginVersion)
//
//    Provider<String> minSupportedGradleVersion = libs.versions.minSupportedGradle
//    inputs.property("minSupportedGradleVersion", minSupportedGradleVersion)
//
//    doLast {
//        constantsKtFile.write(
//                """|package kotlinx.benchmark.gradle.internal
//                |
//                |internal object BenchmarksPluginConstants {
//                |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
//                |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
//                |}
//                |""".stripMargin()
//        )
//    }
//}
//
//sourceSets {
//    main {
//        kotlin.srcDir(generatePluginConstants)
//    }
//}

//apiValidation {
//    nonPublicMarkers += ["kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi"]
//}
