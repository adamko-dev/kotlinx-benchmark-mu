package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.generator.generateJvm
import kotlinx.benchmark.generator.internal.KotlinxBenchmarkGeneratorInternalApi
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@KotlinxBenchmarkPluginInternalApi
internal abstract class JmhBytecodeGeneratorWorker : WorkAction<JmhBytecodeGeneratorWorker.Parameters> {

  @KotlinxBenchmarkPluginInternalApi
  interface Parameters : WorkParameters {
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

    // experimenting with forcing a dry-run...
//    parameters.outputResourceDir.get().asFile.resolve("META-INF/BenchmarkList").apply {
//     writeText(
//       readText()
//         .replace("52 test.generated.KtsTestBenchmark_cosBenchmark_jmhTest", "37 kotlinx.benchmark.jvm.DryRunBenchmark")
//         .replace("52 test.generated.KtsTestBenchmark_sqrtBenchmark_jmhTest", "37 kotlinx.benchmark.jvm.DryRunBenchmark")
//         .replace("12 cosBenchmark", "6 dryRun")
//         .replace("13 sqrtBenchmark", "6 dryRun")
//     )
//    }
  }

  companion object {
    private val logger: Logger = Logging.getLogger(JmhBytecodeGeneratorWorker::class.java)
  }
}
