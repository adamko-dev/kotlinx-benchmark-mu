package kotlinx.benchmark.gradle.mu.tasks.run

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
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
constructor() : BaseRunBenchmarksTask() {

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Input
  abstract val mainClass: Property<String>

//  @get:Nested
//  abstract val javaLauncher: Property<JavaLauncher>

  @get:Input
  @get:Optional
  abstract val ignoreJmhLock: Property<Boolean>

  @get:Nested
  @get:Optional
  abstract val javaLauncher: Property<JavaLauncher>

  @TaskAction
  protected fun action() {
    val encodedBenchmarkParameters = encodeBenchmarkParameters()

//    val reportFile = temporaryDir.resolve("report.${benchmarkParameters.resultFormat.get().extension}")
//
//    val runnerConfig = buildRunnerConfig(
//      name = benchmarkParameters.name,
//      reportFile = reportFile,
//      config = benchmarkParameters,
//      reporting = if (ideaActive.getOrElse(false)) ProgressReporting.IntelliJ else ProgressReporting.Stdout
//    )
//
//    logger.info("[$path] runnerConfig: ${runnerConfig.lines().joinToString(" / ")}")
//
//    val parametersFile = temporaryDir.resolve("parameters.txt").apply {
//      writeText(encodedParameters)
//    }

    val jdkExecutable = this@RunJvmBenchmarkTask.javaLauncher.orNull?.let { jdk ->
      jdk.executablePath.asFile
    }

    logger.info("[$path] runtimeClasspath: ${runtimeClasspath.asPath}")
    val workQueue =
      workers.classLoaderIsolation {
        classpath.from(runtimeClasspath)
      }

    workQueue.submit(RunJvmBenchmarkWorker::class) {
      this.encodedBenchmarkParameters = encodedBenchmarkParameters
      this.classpath = this@RunJvmBenchmarkTask.runtimeClasspath
      this.enableDemoMode = this@RunJvmBenchmarkTask.enableDemoMode
      this.jdkExecutable = jdkExecutable
      this.ignoreJmhLock = this@RunJvmBenchmarkTask.ignoreJmhLock
    }
  }
}
