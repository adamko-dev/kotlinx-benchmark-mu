package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
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
  abstract val runArguments: ListProperty<String>

  @get:OutputDirectory
  abstract val workingDir: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val nodeExecutable: RegularFileProperty

//  @get:InputFile
//  @get:PathSensitive(RELATIVE)
////  @get:Optional
//  //@get:NormalizeLineEndings
//  abstract val module: RegularFileProperty

  @get:PathSensitive(RELATIVE)
  @get:InputFiles
  abstract val module: ConfigurableFileCollection

//  @get:Classpath
//  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Nested
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>

//  @get:Input
//  abstract val mainClass: Property<String>

  @get:Input
  abstract val sourceMapStackTraces: Property<Boolean>

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
      standardOutput = System.out
      errorOutput = System.err
    }
    logger.lifecycle("[$path] Finished running benchmark with Node")
  }

  private fun buildArgs(): List<String> = buildList {

    addAll(module.map { it.absoluteFile.canonicalFile.invariantSeparatorsPath })

    addAll(nodeJsArgs.orNull.orEmpty())

    if (sourceMapStackTraces.get()) {
//      add("--require")
//      add(npmProject.require("source-map-support/register.js"))
    }
  }
}
