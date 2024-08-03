@file:Suppress("UnstableApiUsage")

import kotlin.time.Duration.Companion.seconds


plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.allopen") version "1.9.25"
  id("org.jetbrains.kotlinx.benchmark")
  id("dev.adamko.kotlinx-benchmark")
}

sourceSets.configureEach {
  java.setSrcDirs(listOf("$name/src"))
  resources.setSrcDirs(listOf("$name/resources"))
}

//configure<AllOpenExtension> {
//  annotation("org.openjdk.jmh.annotations.State")
//}

dependencies {
  implementation("dev.adamko.kotlinx-benchmark-mu:kxb-runtime")

  implementation("org.openjdk.jmh:jmh-core:${benchmarks.versions.jmh.get()}")
}

//tasks.withType<JavaCompile> {
//    sourceCompatibility = "1.8"
//    targetCompatibility = "1.8"
//}
//
//
//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//}


benchmarks {

  benchmarkRuns {
    register("main") {
      iterationDuration = 5.seconds
    }
  }

  versions {
    jmh = "1.21"
  }

  targets.kotlinJvm {
  }
}

//benchmark {
//    configurations {
//        named("main") {
//            iterationTime = 5
//            iterationTimeUnit = "sec"
//
//        }
//    }
//    targets {
//        register("main") {
//            this as JvmBenchmarkTarget
//            jmhVersion = "1.21"
//        }
//    }
//}
