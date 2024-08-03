plugins {
  idea
  base
}

idea {
  module {
    excludeDirs.plusAssign(
      listOf(
        ".idea",
        ".kotlin",
//        "build-logic/build-plugins/.kotlin",
      ).map(::File)
    )
//    excludeDirs.plusAssign(
//      listOf(
//        "kotlin-dsl-accessors",
//        "kotlin-dsl-external-plugin-spec-builders",
//        "kotlin-dsl-plugins",
//      ).map { File("build-logic/build-plugins/build/generated-sources/$it/kotlin/gradle/") }
//    )
  }
}

tasks.assemble {
//  dependsOn(gradle.includedBuild("kxb-examples").task(":assemble"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":assemble"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-generator:assemble"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-gradle-plugin:assemble"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-runtime:assemble"))
}

tasks.check {
//  dependsOn(gradle.includedBuild("kxb-examples").task(":assemble"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":check"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-generator:check"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-gradle-plugin:check"))
  dependsOn(gradle.includedBuild("kxb-modules").task(":kxb-runtime:check"))
}

//import kxb.build.tasks.CheckReadmeTask
//
//buildscript {
//    repositories {
//        maven { url 'https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven' }
//        gradlePluginPortal()
//
//        KotlinCommunity.addDevRepositoryIfEnabled(delegate, project)
//    }
//
//    dependencies {
//        classpath(libs.kotlinx.teamInfraGradlePlugin)
//    }
//}
//
//plugins {
//    id("base")
//    alias(libs.plugins.kotlin.multiplatform) apply false
//    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
//}
//
//apply plugin: 'kotlinx.team.infra'
//
//infra {
//    teamcity {
//        libraryStagingRepoDescription = project.name
//    }
//
//    publishing {
//        include(":kotlinx-benchmark-runtime")
//
//        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"
//
//        if (project.findProperty("publication_repository") == "sonatype") {
//            sonatype {}
//        }
//    }
//}
//
//// https://youtrack.jetbrains.com/issue/KT-48410
//repositories {
//    mavenCentral()
//}
//
//// region Workarounds for https://github.com/gradle/gradle/issues/22335
//tasks.register("apiDump") {
//    it.dependsOn(gradle.includedBuild("plugin").task(":apiDump"))
//}
//
//afterEvaluate {
//    gradle.includedBuilds.forEach { included ->
//        project(":kotlinx-benchmark-runtime").tasks.named("publishToMavenLocal") { dependsOn(included.task(":publishToMavenLocal")) }
//    }
//}
////endregion
//
//allprojects {
//    logger.info("Using Kotlin ${libs.versions.kotlin.get()} for project $it")
//    repositories {
//        KotlinCommunity.addDevRepositoryIfEnabled(delegate, project)
//    }
//}
//
//apiValidation {
//    ignoredProjects += [
//            "examples",
//            "java",
//            "kotlin",
//            "kotlin-kts",
//            "kotlin-multiplatform",
//            "integration",
//    ]
//
//    nonPublicMarkers += ["kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi"]
//
//    klib {
//        it.enabled = true
//    }
//}
//
//tasks.register("checkReadme", CheckReadmeTask) {
//    minSupportedGradleVersion = libs.versions.minSupportedGradle
//    readme = file("README.md")
//}
//
//tasks.check {
//    dependsOn(tasks.named("checkReadme"))
//}
