package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.generator.generateJvm
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class GenerateJvmJmhSourceWorker : WorkAction<GenerateJvmJmhSourceWorker.Parameters> {

  internal interface Parameters : WorkParameters {
    val inputClasses: ConfigurableFileCollection
    val inputClasspath: ConfigurableFileCollection
    val outputSourceDir: DirectoryProperty
    val outputResourceDir: DirectoryProperty
  }

  override fun execute() {
    @OptIn(KotlinxBenchmarkGeneratorInternalApi::class)
    generateJvm(
      inputClasses = parameters.inputClasses.files,
      inputClasspath = parameters.inputClasspath.files,
      outputSourceDirectory = parameters.outputSourceDir.get().asFile,
      outputResourceDirectory = parameters.outputResourceDir.get().asFile,
      logger = logger::info,
    )
  }

  companion object {
    private val logger: Logger = Logging.getLogger(GenerateJvmJmhSourceWorker::class.java)
  }
}
