package kotlinx.benchmark.gradle.mu.tasks.run

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
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
abstract class RunJsD8BenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : RunBenchmarkBaseTask() {

  /** Arguments for D8. Will be passed first, before [runArguments]. */
  @get:Input
  abstract val d8Arguments: ListProperty<String>

  /** Arguments for the program being executed. Will be passed last, after `--`. */
  @get:Input
  @get:Optional
  abstract val runArguments: ListProperty<String>

  @get:OutputDirectory
  abstract val workingDir: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val executable: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val module: RegularFileProperty

  @TaskAction
  protected fun action() {

  }
}
