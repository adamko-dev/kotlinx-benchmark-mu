package kotlinx.benchmark.gradle.mu.tasks.run

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.work.DisableCachingByDefault

/**
 * Run JS Benchmarks using NodeJS.
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
    }

    logger.lifecycle("[$path] Finished running benchmark with Node")
  }

  private fun buildArgs(): List<String> {
    val encodedBenchmarkParameters = encodeBenchmarkParameters()

    return buildList {
      addAll(requiredJsFiles.flatMap {
        listOf(
          "-r",
          it.absoluteFile.canonicalFile.invariantSeparatorsPath
        )
      })
      //if (sourceMapStackTraces.get()) {
      //        add("--require")
      //        add("-r")
      //        add("source-map-support/register")
      //        add("source-map-support/register.js")
      //        add(npmProject.require("source-map-support/register.js"))
      //}

      addAll(nodeJsArgs.orNull.orEmpty())

      addAll(module.map { it.absoluteFile.canonicalFile.invariantSeparatorsPath })

      add(encodedBenchmarkParameters)
    }
  }
}
