package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.generator.generateJs
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import kotlinx.benchmark.gradle.mu.config.JsBenchmarksExecutor
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Generates JavaScript benchmarking source code.
 *
 * This worker requires `kotlin-compiler-embeddable` and *must* be run in an isolated classpath.
 *
 * @see kotlinx.benchmark.gradle.mu.tasks.JsSourceGeneratorTask
 */
internal abstract class GenerateJsSourceWorker : WorkAction<GenerateJsSourceWorker.Params> {

  internal interface Params : WorkParameters {
    val title: Property<String>
    val inputClasses: ConfigurableFileCollection
    val inputDependencies: ConfigurableFileCollection
    val outputSourcesDir: DirectoryProperty
    val outputResourcesDir: DirectoryProperty
    val benchmarksExecutor: Property<JsBenchmarksExecutor>
  }

  override fun execute() {
    parameters.outputSourcesDir.get().asFile.deleteRecursively()
    parameters.outputResourcesDir.get().asFile.deleteRecursively()

    @OptIn(KotlinxBenchmarkGeneratorInternalApi::class)
    generateJs(
      title = parameters.title.get(),
      inputClasses = parameters.inputClasses.files,
      inputDependencies = parameters.inputDependencies.files,
      outputSourcesDir = parameters.outputSourcesDir.get().asFile,
      outputResourcesDir = parameters.outputResourcesDir.get().asFile,
      useBenchmarkJs = parameters.benchmarksExecutor.get() == JsBenchmarksExecutor.BenchmarkJs,
    )
  }
}
