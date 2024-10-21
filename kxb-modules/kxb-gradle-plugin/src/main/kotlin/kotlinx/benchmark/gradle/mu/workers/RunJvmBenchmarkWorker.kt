package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.jvm.runJvmBenchmark
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class RunJvmBenchmarkWorker : WorkAction<RunJvmBenchmarkWorker.Parameters> {

  internal interface Parameters : WorkParameters {
    val encodedBenchmarkParameters: Property<String>
    val classpath: ConfigurableFileCollection
    val ignoreJmhLock: Property<Boolean>
    val enableDemoMode: Property<Boolean>
    val jdkExecutable: RegularFileProperty
  }

  @OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
  override fun execute() {
    val isDemoMode = parameters.enableDemoMode.getOrElse(false)
    val ignoreJmhLock = parameters.ignoreJmhLock.getOrElse(false)

    val encodedBenchmarkParameters = parameters.encodedBenchmarkParameters.get()

    if (isDemoMode || ignoreJmhLock) {
      System.setProperty("jmh.ignoreLock", "true")
    }

    System.setProperty("java.class.path", parameters.classpath.asPath)
    logger.info("java.class.path ${System.getProperty("java.class.path")}")

    runJvmBenchmark(
      config = RunnerConfiguration.decodeFromBase64Json(encodedBenchmarkParameters),
      demoMode = isDemoMode,
      jdkExecutable = parameters.jdkExecutable.orNull?.asFile,
    )
  }

  companion object {
    private val logger: Logger = Logging.getLogger(RunJvmBenchmarkWorker::class.java)
  }
}
