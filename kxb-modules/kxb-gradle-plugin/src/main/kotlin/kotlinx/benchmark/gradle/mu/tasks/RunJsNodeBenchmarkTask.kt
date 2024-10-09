package kotlinx.benchmark.gradle.mu.tasks

import java.io.File
import javax.inject.Inject
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec.Companion.buildRunnerConfig
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.work.DisableCachingByDefault

/**
 * Run JS Benchmarks using D8.
 */
@DisableCachingByDefault(because = "wip")
abstract class RunJsNodeBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : RunBenchmarkBaseTask() {

  /** Arguments for Node. Will be passed first, before [runArguments]. */
  @get:Input
  abstract val nodeJsArgs: ListProperty<String>

  /** Arguments for the program being executed. Will be passed last, after `--`. */
  @get:Input
  @get:Optional
  abstract val runArguments: ListProperty<String> // TODO implement runArguments

//  @get:OutputDirectory
//  abstract val workingDir: DirectoryProperty

  @get:OutputFile
  abstract val results: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val nodeExecutable: RegularFileProperty

//  @get:InputFile
//  @get:PathSensitive(RELATIVE)
////  @get:Optional
//  //@get:NormalizeLineEndings
//  abstract val module: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val module: ConfigurableFileCollection

  @get:Nested
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>

  @get:Input
  abstract val sourceMapStackTraces: Property<Boolean>

  @get:LocalState
  abstract val cacheDir: DirectoryProperty

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val requiredJsFiles: ConfigurableFileCollection

  @TaskAction
  protected fun action() {
    val nodeExecutable = nodeExecutable.get().asFile
    val benchmarkArgs = buildArgs()

    logger.lifecycle(
      """
        [$path] running benchmark with Node...
            nodeExecutable:${nodeExecutable}
            benchmarkArgs:${benchmarkArgs}
            module:${module.files}
      """.trimIndent()
    )

    exec.exec {
      executable(nodeExecutable)
      args(benchmarkArgs)
//      standardOutput = System.out
//      errorOutput = System.err
    }

    logger.lifecycle("[$path] Finished running benchmark with Node")
  }

  private fun buildArgs(): List<String> {

    val benchmarkParameters = createBenchmarkParametersFile()

    return buildList {
      addAll(requiredJsFiles.flatMap {
        listOf(
          "-r",
          it.absoluteFile.canonicalFile.invariantSeparatorsPath
        )
      })
      if (sourceMapStackTraces.get()) {
//              add("--require")
//              add("-r")
//              add("source-map-support/register")
//              add("source-map-support/register.js")
//              add(npmProject.require("source-map-support/register.js"))
      }

      addAll(nodeJsArgs.orNull.orEmpty())

      addAll(module.map { it.absoluteFile.canonicalFile.invariantSeparatorsPath })

      add(benchmarkParameters.invariantSeparatorsPath)

    }
  }

  private fun createBenchmarkParametersFile(): File {

    val benchmarkParameters = benchmarkParameters.get()

    val reportFile = results.get().asFile.apply {
      parentFile.mkdirs()
    }

    val runnerConfig = buildRunnerConfig(
      name = benchmarkParameters.name,
      reportFile = reportFile,
      config = benchmarkParameters,
      reporting = if (ideaActive.getOrElse(false)) ProgressReporting.IntelliJ else ProgressReporting.Stdout
    )

    val benchmarkParametersFile = cacheDir.get().asFile.resolve("benchmarkParameters.json").apply {
      parentFile.mkdirs()
      writeText(runnerConfig)
    }

    logger.lifecycle(
      "[$path] benchmarkParameters ${benchmarkParametersFile.toURI()}: " +
          benchmarkParametersFile.useLines { it.joinToString(" / ") }
    )

    return benchmarkParametersFile
  }
}
