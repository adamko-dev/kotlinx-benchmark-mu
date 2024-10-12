package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.generator.generateNative
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class GenerateNativeSourceWorker : WorkAction<GenerateNativeSourceWorker.Parameters> {

  internal interface Parameters : WorkParameters {
    val title: Property<String>
    val target: Property<String>
    val inputClasses: ConfigurableFileCollection
    val inputDependencies: ConfigurableFileCollection
    val outputSourcesDir: DirectoryProperty
    val outputResourcesDir: DirectoryProperty
  }

  override fun execute() {
    @OptIn(KotlinxBenchmarkGeneratorInternalApi::class)
    generateNative(
      title = parameters.title.get(),
      target = parameters.target.get(),
      inputClassesDirs = parameters.inputClasses.files,
      inputDependencies = parameters.inputDependencies.files,
      outputSourcesDir = parameters.outputSourcesDir.get().asFile,
      outputResourcesDir = parameters.outputResourcesDir.get().asFile,
    )
  }
}
