package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.mu.config.KotlinJvmBenchmarkTarget
import org.gradle.api.Project

internal fun Project.processJvmCompilation(target: KotlinJvmBenchmarkTarget) {
  project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/JVM")
  val compilation = target.compilation

  configureMultiplatformJvmCompilation(target)

//    val workerClasspath = this.createJmhGenerationRuntimeConfiguration(target.name, target.jmhVersion)
  createJvmBenchmarkGenerateSourceTask(
    target = target,
//        workerClasspath,
    compileClasspath = compilation.compileDependencyFiles,
    compilationTask = compilation.compileAllTaskName,
    compilationOutput = compilation.output.allOutputs
  )
  val runtimeClasspath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
  createJvmBenchmarkCompileTask(target, runtimeClasspath)
//    target.extension.configurations.forEach {
//        createJvmBenchmarkExecTask(it, target, runtimeClasspath)
//    }
}

private fun Project.configureMultiplatformJvmCompilation(target: KotlinJvmBenchmarkTarget) {
//    // Add JMH core library as an implementation dependency to the specified compilation
//    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}:${target.jmhVersion}")
//
//    target.compilation.dependencies {
//        implementation(jmhCore)
//    }
}
