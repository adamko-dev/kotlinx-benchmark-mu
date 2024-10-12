package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec.Companion.buildRunnerConfig
import kotlinx.benchmark.gradle.mu.workers.RunJvmBenchmarkWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class RunJvmBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : RunBenchmarkBaseTask() {

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Nested
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>

  @get:Input
  abstract val mainClass: Property<String>

//  @get:Nested
//  abstract val javaLauncher: Property<JavaLauncher>

  // TODO add jmh.ignoreLock Boolean option

  // TODO pass java launcher to JMH
  @get:Nested
  abstract val javaLauncher: Property<JavaLauncher>

  @TaskAction
  protected fun action() {
    val benchmarkParameters = benchmarkParameters.get()

    val reportFile = temporaryDir.resolve("report.${benchmarkParameters.resultFormat.get().extension}")

    val runnerConfig = buildRunnerConfig(
      name = benchmarkParameters.name,
      reportFile = reportFile,
      config = benchmarkParameters,
      reporting = if (ideaActive.getOrElse(false)) ProgressReporting.IntelliJ else ProgressReporting.Stdout
    )

    logger.info("[$path] runnerConfig: ${runnerConfig.lines().joinToString(" / ")}")

//    val parametersFile = temporaryDir.resolve("parameters.txt").apply {
//      writeText(encodedParameters)
//    }

    logger.info("[$path] runtimeClasspath: ${runtimeClasspath.asPath}")
    val workQueue =
      workers.classLoaderIsolation {
        classpath.from(runtimeClasspath)
      }

    workQueue.submit(RunJvmBenchmarkWorker::class) {
      this.config = runnerConfig
      this.classpath = runtimeClasspath
      this.enableDemoMode = this@RunJvmBenchmarkTask.enableDemoMode
    }
  }
}
