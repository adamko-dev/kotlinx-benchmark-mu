package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import org.gradle.jvm.tasks.*
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import kotlinx.benchmark.gradle.mu.config.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.mu.tasks.generate.GenerateJvmBenchmarkTask
import org.gradle.kotlin.dsl.register

internal fun Project.createJvmBenchmarkCompileTask(target: JvmBenchmarkTarget, compileClasspath: FileCollection) {
//    val benchmarkBuildDir = benchmarkBuildDir(target)
    val compileTask = tasks.register<JavaCompile>(
        "${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}",
    ) {
        dependsOn()
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Compile JMH source files for '${target.name}'"
        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
        classpath = compileClasspath
//        source = fileTree("$benchmarkBuildDir/sources")
//        destinationDirectory.set(file("$benchmarkBuildDir/classes"))
        javaCompiler.set(javaCompilerProvider())
    }

    val jarTask = tasks.register<Jar>(
        "${target.name}${BenchmarksPlugin.BENCHMARK_JAR_SUFFIX}",
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Build JAR for JMH compiled files for '${target.name}'"
        isZip64 = true
        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        archiveClassifier.set("JMH")
        manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"

        duplicatesStrategy = DuplicatesStrategy.WARN

        from(project.provider {
            compileClasspath.map {
                when {
                    it.isDirectory -> it
                    it.exists() -> zipTree(it).let { tree ->
                        if (it.name.startsWith("kotlin-stdlib-jdk")) {
                            tree.filter { file ->
                                !(file.toString().contains("META-INF") && file.name in listOf("module-info.class", "MANIFEST.MF"))
                            }
                        } else {
                            tree
                        }
                    }
                    else -> files()
                }
            }
        })

        from(compileTask)
//        from(file("$benchmarkBuildDir/resources"))
//        destinationDirectory.set(File("$benchmarkBuildDir/jars"))
        archiveBaseName.set("${project.name}-${target.name}-jmh")
    }

    tasks.named(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME) {
        dependsOn(compileTask)
        dependsOn(jarTask)
    }
}

internal fun Project.createJmhGenerationRuntimeConfiguration(name: String, jmhVersion: String): Configuration {
    // This configuration defines classpath for JMH generator, it should have everything available via reflection
    return configurations.create("$name${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}CP").apply {
        isVisible = false
        description = "JMH Generator Runtime Configuration for '$name'"

        val dependencies = this@createJmhGenerationRuntimeConfiguration.dependencies
//        (defaultDependencies {
//            it.add(dependencies.create("${BenchmarksPlugin.JMH_GENERATOR_DEPENDENCY}$jmhVersion"))
//        })
    }
}

internal fun Project.createJvmBenchmarkGenerateSourceTask(
    target: BenchmarkTarget,
//    workerClasspath: FileCollection,
    compileClasspath: FileCollection,
    compilationTask: String,
    compilationOutput: FileCollection
) {
//    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<GenerateJvmBenchmarkTask>("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JMH source files for '${target.name}'"
        dependsOn(compilationTask)
//        runtimeClasspath = workerClasspath
//        inputCompileClasspath = compileClasspath
//        inputClassesDirs = compilationOutput
//        outputResourcesDir = file("$benchmarkBuildDir/resources")
//        outputSourcesDir = file("$benchmarkBuildDir/sources")
//        executableProvider = javaLauncherProvider().map {
//            it.executablePath.asFile.absolutePath
//        }
    }
}

internal fun Project.createJvmBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JvmBenchmarkTarget,
    runtimeClasspath: FileCollection
) {
    // TODO: add working dir parameter?
    val execTask = tasks.register<JavaExec>(
        "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Execute benchmark for '${target.name}'"

//        val benchmarkBuildDir = benchmarkBuildDir(target)
        mainClass.set("kotlinx.benchmark.jvm.JvmBenchmarkRunnerKt")

//        if (target.workingDir != null)
//            workingDir = File(target.workingDir)

        classpath(
//            file("$benchmarkBuildDir/classes"),
//            file("$benchmarkBuildDir/resources"),
            runtimeClasspath
        )

        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")

//        val reportFile = setupReporting(target, config)
//        args(writeParameters(target.name, reportFile, traceFormat(), config))
        javaLauncher.set(javaLauncherProvider())
    }


  tasks.named(config.prefixName(BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME)) {
    dependsOn(execTask)
  }
}
