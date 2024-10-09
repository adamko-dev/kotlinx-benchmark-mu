package kotlinx.benchmark.gradle.mu.tasks

import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.RunnerConfiguration.Companion.encodeToJson
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
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
  abstract val benchmarkParameters: Property<BenchmarkRunSpec>// =
//    extensions.adding("benchmarkParameters", objects.newInstance("test1"))

  @KotlinxBenchmarkPluginInternalApi
  @get:Input
  abstract val mainClass: Property<String>

//  @get:Nested
//  abstract val javaLauncher: Property<JavaLauncher>

  // TODO add jmh.ignoreLock Boolean option

  // TODO pass java launcher to JMH
  @get:Nested
  abstract val javaLauncher: Property<JavaLauncher>

  @TaskAction
  fun action() {
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


private fun buildRunnerConfig(
  name: String,
  reportFile: File,
  config: BenchmarkRunSpec,
  reporting: ProgressReporting
): String {
  validateConfig(config)

  return RunnerConfiguration(
    name = name,
    resultFilePath = reportFile.invariantSeparatorsPath
  ) {
//    name = name,
//    reportFile = reportFile.invariantSeparatorsPath,
//    traceFormat = traceFormat,
    progressReporting = reporting
    includes += config.includes.orNull.orEmpty()
    excludes += config.excludes.orNull.orEmpty()
    measurementIterations = config.iterations.orNull
    measurementDuration = config.iterationDuration.orNull
//    iterationDuration = config.iterationDuration.orNull
//        batchSize = config.batchSize,
//        runTime = config.runTime,
    warmupIncludes += config.warmupBenchmarks.orNull.orEmpty()
    warmupIterations = config.warmupIterations.orNull
//        warmupBatchSize = config.warmupBatchSize,
    warmupForks = config.warmupForks.orNull
    warmupDuration = config.warmupTime.orNull
    timeout = config.timeout.orNull
    threads = config.threads.orNull
    enableSyncIterations = config.synchronizeIterations.orNull
    enableGcPerIteration = config.gcEachIteration.orNull
    failOnError = config.failOnError.orNull
    forks = config.forks.orNull
//        threadGroups = config.threadGroups,
//        opsPerInvocation = config.opsPerInvocation,
    resultTimeUnit = config.resultTimeUnit.orNull
//      ?.let {
//      when (it) {
//        ResultTimeUnit.Minutes      -> RunnerConfiguration.ReportTimeUnit.Minutes
//        ResultTimeUnit.Microseconds -> RunnerConfiguration.ReportTimeUnit.Microseconds
//        ResultTimeUnit.Milliseconds -> RunnerConfiguration.ReportTimeUnit.Milliseconds
//        ResultTimeUnit.Nanoseconds  -> RunnerConfiguration.ReportTimeUnit.Nanoseconds
//        ResultTimeUnit.Seconds      -> RunnerConfiguration.ReportTimeUnit.Seconds
//        is ResultTimeUnit.Custom    -> TODO()
//      }
//    }
    mode = config.mode.orNull
    /*?.let {
    when (it) {
      BenchmarkMode.All            -> RunnerConfiguration.Mode.All
      BenchmarkMode.AverageTime    -> RunnerConfiguration.Mode.AverageTime
      is BenchmarkMode.Custom      -> TODO()
      BenchmarkMode.SampleTime     -> RunnerConfiguration.Mode.SampleTime
      BenchmarkMode.SingleShotTime -> RunnerConfiguration.Mode.SingleShotTime
      BenchmarkMode.Throughput     -> RunnerConfiguration.Mode.Throughput
    }
  }*/
//    profilers = config.profilers.orNull,
    resultFormat = config.resultFormat.get()
//      .let {
//      when (it) {
//        ResultFormat.CSV       -> RunnerConfiguration.ResultFormat.CSV
//        ResultFormat.JSON      -> RunnerConfiguration.ResultFormat.JSON
//        ResultFormat.SCSV      -> RunnerConfiguration.ResultFormat.SCSV
//        ResultFormat.Text      -> RunnerConfiguration.ResultFormat.Text
//        is ResultFormat.Custom -> TODO()
//      }
//    }
    jvmArgs += config.jvmArgs.orNull.orEmpty()
    parameters += config.parameters.orNull.orEmpty()
//    advanced = config.advanced.orNull.orEmpty(),
  }.encodeToJson()

////  val file = Files.createTempFile("benchmarks", "txt").toFile()
//  return buildString {
//    appendLine("name:$name")
//    appendLine("reportFile:$reportFile")
//    appendLine("traceFormat:$traceFormat")
//    config.reportFormat.orNull?.let { appendLine("reportFormat:${it.format}") }
//    config.iterations.orNull?.let { appendLine("iterations:$it") }
//    config.warmups.orNull?.let { appendLine("warmups:$it") }
//    config.iterationDuration.orNull?.let {
//      appendLine("iterationTime:${it.inWholeMilliseconds}")
//      appendLine("iterationTimeUnit:${DurationUnit.MILLISECONDS}")
//    }
//    config.reportTimeUnit.orNull?.let { appendLine("outputTimeUnit:${it.unit}") }
//
//    config.mode.orNull?.let { appendLine("mode:${it.id}") }
//
//    config.includes.orNull?.forEach {
//      appendLine("include:$it")
//    }
//    config.excludes.orNull?.forEach {
//      appendLine("exclude:$it")
//    }
//    config.params.orNull?.forEach { (param, values) ->
//      values.forEach { value ->
//        appendLine("param:$param=$value")
//      }
//    }
//    config.advanced.orNull?.forEach { (param, value) ->
//      appendLine("advanced:$param=$value")
//    }
//  }
}

private fun validateConfig(config: BenchmarkRunSpec) {
//  config.reportFormat.orNull?.let {
//    require(it.lowercase() in ValidOptions.format) {
//      "Invalid report format: '$it'. Accepted formats: ${ValidOptions.format.joinToString(", ")} (e.g., reportFormat = \"json\")."
//    }
//  }

  config.iterations.orNull?.let {
    require(it > 0) {
      "Invalid iterations: '$it'. Expected a positive integer (e.g., iterations = 5)."
    }
  }

  config.warmups.orNull?.let {
    require(it >= 0) {
      "Invalid warmups: '$it'. Expected a non-negative integer (e.g., warmups = 3)."
    }
  }

  config.iterationDuration.orNull?.let {
    require(it > Duration.ZERO) {
      "Invalid iterationTime: '$it'. Must be greater than ${Duration.ZERO} (e.g., iterationTime = 300.seconds)."
    }
  }

//  config.mode.orNull?.let {
//    require(it in ValidOptions.modes) {
//      "Invalid benchmark mode: '$it'. Accepted modes: ${ValidOptions.modes.joinToString(", ")} (e.g., mode = \"thrpt\")."
//    }
//  }

//  config.outputTimeUnit.orNull?.let {
//    require(it in ValidOptions.timeUnits) {
//      "Invalid outputTimeUnit: '$it'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., outputTimeUnit = \"ns\")."
//    }
//  }

  config.includes.orNull?.forEach { pattern ->
    require(pattern.isNotBlank()) {
      "Invalid include pattern: '$pattern'. Pattern must not be blank."
    }
  }

  config.excludes.orNull?.forEach { pattern ->
    require(pattern.isNotBlank()) {
      "Invalid exclude pattern: '$pattern'. Pattern must not be blank."
    }
  }

  config.parameters.orNull?.forEach { (param, values) ->
    require(param.isNotBlank()) {
      "Invalid parameter name: '$param'. It must not be blank."
    }
    require(values.isNotEmpty()) {
      "Parameter '$param' has no values. At least one value is required."
    }
  }

  config.advanced.orNull?.forEach { (param, value) ->
    require(param.isNotBlank()) {
      "Invalid advanced option name: '$param'. It must not be blank."
    }
    require(value.toString().isNotBlank()) {
      "Invalid value for advanced option '$param': '$value'. Value should not be blank."
    }

    when (param) {
//      "nativeFork"             -> {
//        require(value.toString() in ValidOptions.nativeForks) {
//          "Invalid value for 'nativeFork': '$value'. Accepted values: ${ValidOptions.nativeForks.joinToString(", ")}."
//        }
//      }

      "nativeGCAfterIteration" -> require(value.toBooleanStrictOrNull() != null) {
        "Invalid value for 'nativeGCAfterIteration': '$value'. Expected a Boolean value."
      }

      "jvmForks"               -> {
        val intValue = value.toIntOrNull()
        require(intValue != null && intValue >= 0 || value.toString() == "definedByJmh") {
          "Invalid value for 'jvmForks': '$value'. Expected a non-negative integer or \"definedByJmh\"."
        }
      }

      "jsUseBridge"            -> require(value.toBooleanStrictOrNull() != null) {
        "Invalid value for 'jsUseBridge': '$value'. Expected a Boolean value."
      }

//      else                     -> throw IllegalArgumentException("Invalid advanced option name: '$param'. Accepted options: \"nativeFork\", \"nativeGCAfterIteration\", \"jvmForks\", \"jsUseBridge\".")
    }
  }
}

//private object ValidOptions {
//  val format = setOf("json", "csv", "scsv", "text")
//  val timeUnits = setOf(
//    "NANOSECONDS", "ns", "nanos",
//    "MICROSECONDS", "us", "micros",
//    "MILLISECONDS", "ms", "millis",
//    "SECONDS", "s", "sec",
//    "MINUTES", "m", "min"
//  )
//  val modes = setOf("thrpt", "avgt", "Throughput", "AverageTime")
//  val nativeForks = setOf("perBenchmark", "perIteration")
//}
