package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.generator.generateWasm
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Generates Wasm benchmarking source code.
 *
 * This worker requires `kotlin-compiler-embeddable` and *must* be run in an isolated classpath.
 *
 * @see kotlinx.benchmark.gradle.mu.tasks.WasmSourceGeneratorTask
 */
internal abstract class GenerateWasmSourceWorker : WorkAction<GenerateWasmSourceWorker.Params> {

  internal interface Params : WorkParameters {
    val title: Property<String>
    val inputClasses: ConfigurableFileCollection
    val inputDependencies: ConfigurableFileCollection
    val outputSourcesDir: DirectoryProperty
    val outputResourcesDir: DirectoryProperty
  }

  override fun execute() {
    @OptIn(KotlinxBenchmarkGeneratorInternalApi::class)
    generateWasm(
      title = parameters.title.get(),
      inputClasses = parameters.inputClasses.files,
      inputDependencies = parameters.inputDependencies.files,
      outputSourcesDir = parameters.outputSourcesDir.get().asFile,
      outputResourcesDir = parameters.outputResourcesDir.get().asFile,
    )
  }
}
