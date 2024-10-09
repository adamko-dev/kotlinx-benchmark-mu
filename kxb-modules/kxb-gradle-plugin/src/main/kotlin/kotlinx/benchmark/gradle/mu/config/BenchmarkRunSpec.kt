package kotlinx.benchmark.gradle.mu.config

import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.RunnerConfiguration.Companion.encodeToJson
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.internal.utils.DurationJdk
import org.gradle.api.Named
import org.gradle.api.provider.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class BenchmarkRunSpec
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(private val name: String) : Named {

  @get:Input
  @get:Optional
  abstract val enabled: Property<Boolean> // TODO enable/disable BenchmarkRun

  @get:Input
  @get:Optional
  abstract val iterations: Property<Int>
  @get:Input
  @get:Optional
  abstract val forks: Property<Int>
  @get:Input
  @get:Optional
  abstract val warmupIterations: Property<Int>
  @get:Input
  @get:Optional
  abstract val threads: Property<Int>

  @get:Input
  @get:Optional
  abstract val failOnError: Property<Boolean>
  @get:Input
  @get:Optional
  abstract val gcEachIteration: Property<Boolean>
  @get:Input
  @get:Optional
  abstract val synchronizeIterations: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val warmups: Property<Int>

  @get:Input
  @get:Optional
  abstract val warmupForks: Property<Int>

  @get:Input
  @get:Optional
  abstract val timeout: Property<Duration>

  @get:Input
  @get:Optional
  abstract val warmupTime: Property<Duration>

  @get:Internal // Cannot fingerprint input property 'iterationDuration': value '300ms' cannot be serialized.
  abstract val iterationDuration: Property<Duration>

  @get:Input
  @get:Optional
  @get:JvmSynthetic
  @KotlinxBenchmarkPluginInternalApi
  @Deprecated(level = DeprecationLevel.HIDDEN, message = "Because Gradle can't serializer kotlin.time.Duration")
  @Suppress("unused")
  protected val iterationDurationProvider: Provider<DurationJdk>
    get() = iterationDuration.map { it.toJavaDuration() }

  @get:Input
  @get:Optional
  abstract val mode: Property<RunnerConfiguration.Mode>

  @get:Input
  abstract val resultFormat: Property<RunnerConfiguration.ResultFormat>

  @get:Input
  @get:Optional
  abstract val resultTimeUnit: Property<RunnerConfiguration.ReportTimeUnit>

  @get:Input
  @get:Optional
  abstract val includes: SetProperty<String>

  @get:Input
  @get:Optional
  abstract val excludes: SetProperty<String>
  @get:Input
  @get:Optional
  abstract val profilers: SetProperty<String>
  @get:Input
  @get:Optional
  abstract val jvmArgs: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val warmupBenchmarks: SetProperty<String>

  // todo typesafe params...
  @get:Input
  @get:Optional
  abstract val parameters: MapProperty<String, List<String>>

  @get:Input
  // todo typesafe advanced...
  @get:Optional
  abstract val advanced: MapProperty<String, String>

  fun include(pattern: String) {
    includes.add(pattern)
  }

  fun exclude(pattern: String) {
    excludes.add(pattern)
  }

  //  fun param(name: String, vararg value: Any?) {
//    val values = params.getOrPut(name) { mutableListOf() }
//    values.addAll(value)
//  }
//
//  fun advanced(name: String, value: Any?) {
//    advanced[name] = value
//  }
//
  @Input
  override fun getName(): String = name


  fun iterationDuration(duration: DurationJdk) {
    iterationDuration.set(duration.toKotlinDuration())
  }

  fun iterationDuration(duration: Provider<DurationJdk>) {
    iterationDuration.set(duration.map { it.toKotlinDuration() })
  }

//  fun iterationDuration(duration: Duration) {
//    iterationDuration.set(duration)
//  }
//
//  @JvmName("iterationDurationKt")
//  fun iterationDuration(duration: Provider<Duration>) {
//    iterationDuration.set(duration.map { it })
//  }

  /**
   * Generate special benchmark bridges to stop inlining optimizations.
   *
   * This value is only valid for JavaScript targets.
   */
  @get:Input
  @get:Optional
  abstract val enableJsBridges: Property<Boolean>


  companion object {
    internal fun buildRunnerConfig(
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
        enableJsBridges = config.enableJsBridges.orNull
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

  }
}
