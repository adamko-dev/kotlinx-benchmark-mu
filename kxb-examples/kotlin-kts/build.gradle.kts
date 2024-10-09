@file:Suppress("UnstableApiUsage")

import kotlin.time.Duration.Companion.seconds


plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.allopen") version "1.9.25"
  id("org.jetbrains.kotlinx.benchmark")
  id("dev.adamko.kotlinx-benchmark")
}

//configure<AllOpenExtension> {
//  annotation("org.openjdk.jmh.annotations.State")
//}

dependencies {
  implementation("dev.adamko.kotlinx-benchmark-mu:kxb-runner")

  // TODO auto-add jmh-core dependency...
  implementation("org.openjdk.jmh:jmh-core:${benchmark.versions.jmh.get()}")
}

//tasks.withType<JavaCompile> {
//    sourceCompatibility = "1.8"
//    targetCompatibility = "1.8"
//}
//
//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//}

benchmark {

  benchmarkRuns {
    create("main") {
      warmups = 0
      iterations = 3
      iterationDuration = 5.seconds
    }
  }

//  versions {
//    jmh = "1.21"
//  }

  targets {
    jvm {
      //this.compileTask
    }
    println("${project.displayName} - targets:${targets.names}")
//    jvm {
//
//    }
//    jvm {
//    }
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
