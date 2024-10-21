package kotlinx.benchmark

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlinx.benchmark.RunnerConfiguration.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface RunnerConfiguration {
  val name: String

  /** Which file to use for reporting the result. */
  val resultFilePath: String

  val parameters: Map<String, List<String>>

  val progressReporting: ProgressReporting?

  /**
   * Generate special benchmark bridges to stop inlining optimizations.
   *
   * This property is only valid for JavaScript targets.
   */
  val enableJsBridge: Boolean?

  /**
   * Which benchmarks to execute?
   * A list of regexps matching the requested benchmarks
   */
  val includes: List<String>

  /**
   * Which benchmarks to omit?
   * A list of regexps matching the ignored benchmarks.
   */
  val excludes: List<String>

  /** Result format to use. */
  val resultFormat: ResultFormat

//  /**
//   * Which file to use for dumping the result
//   * @return file name
//   */
//  val result: String?

  /** Enable GC between iterations */
  val enableGcPerIteration: Boolean?

///**
// * Profilers to use for the run.
// * Profilers will start in the order specified by collection, and will stop in the reverse order.
// * @return profilers to use; empty collection if no profilers are required
// */
//val profilers:  List<ProfilerConfig>

  /**
   * How verbose should we be?
   */
  val verbosity: VerboseMode?

  /**
   * Should harness terminate on first error encountered?
   * @return should terminate?
   */
  val failOnError: Boolean?

  /**
   * Number of threads to run
   * @return number of threads; 0 to use maximum number of threads
   */
  val threads: Int?

  /** Thread subgroups distribution. An array of thread ratios. */
  val threadGroups: List<Int>

  /** Should synchronize iterations? */
  val enableSyncIterations: Boolean?

  /** Number of warmup iterations. */
  val warmupIterations: Int?

  /** The duration for warmup iterations. */
  val warmupDuration: Duration?

  /** The warmup batch size. */
  val warmupBatchSize: Int?

  /** Warmup mode. */
  val warmupMode: WarmupMode?

  /**
   * Which benchmarks to warmup before doing the run.
   *
   * List of regexps matching the relevant benchmarks; empty if no benchmarks are defined.
   */
  val warmupIncludes: List<String>

  /** Number of measurement iterations. */
  val measurementIterations: Int?

  /** The duration for measurement iterations */
  val measurementDuration: Duration?

  /** Number of batches for measurement */
  val measurementBatchSize: Int?

  /** Mode to execute the benchmark in. */
  val mode: Mode?

  /** Time unit to use in the report. */
  val resultTimeUnit: ReportTimeUnit?

  /** Number of operations per test invocation. */
  val operationsPerInvocation: Int?

  /** Fork count. 0, to prohibit forking. */
  val forks: Int?

  /** Number of initial forks to ignore the results for. 0, to disable. */
  val warmupForks: Int?

  /** JVM executable to use for forks */
  val jvm: String?

  /** JVM parameters to use with forks. */
  val jvmArgs: List<String>

  /**
   * JVM parameters to use with forks.
   * (These options will be appended after any other JVM options.)
   */
  val jvmArgsAppend: List<String>

  /**
   * JVM parameters to use with forks.
   * (These options will be prepended before any other JVM options.)
   */
  val jvmArgsPrepend: List<String>

  /** Timeout: how long to wait for an iteration to complete. */
  val timeout: Duration?

  val nativeFork: NativeFork?

  enum class Mode {

    /**
     * Throughput: operations per unit of time.
     *
     * Runs by continuously calling [@Benchmark][kotlinx.benchmark.Benchmark] methods,
     * counting the total throughput over all worker threads.
     *
     * This mode is time-based, and it will run until the iteration time expires.
     */
    Throughput,

    /**
     * Average time: average time per operation.
     *
     * Runs by continuously calling [@Benchmark][kotlinx.benchmark.Benchmark] methods,
     * counting the average time to call over all worker threads.
     *
     * This is the inverse of [Throughput], but with different aggregation policy.
     * This mode is time-based, and it will run until the iteration time expires.
     */
    AverageTime,

    /**
     * Sample time: samples the time for each operation.
     *
     * Runs by continuously calling [@Benchmark][kotlinx.benchmark.Benchmark] methods,
     * and randomly sampling the time needed for the call.
     *
     * This mode automatically adjusts the sampling frequency,
     * but may omit some pauses which missed the sampling measurement.
     *
     * This mode is time-based, and it will run until the iteration time expires.
     */
    SampleTime,

    /**
     *
     * Single shot time: measures the time for a single operation.
     *
     * Runs by calling [@Benchmark][kotlinx.benchmark.Benchmark] once and measuring its time.
     *
     * This mode is useful to estimate the "cold" performance when you don't want to hide the
     * warmup invocations, or if you want to see the progress from call to call,
     * or you want to record every single sample.
     *
     * This mode is work-based, and will run only for a single invocation of `@Benchmark` method.
     *
     * Caveats for this mode include:
     *
     * - More warmup/measurement iterations are generally required.
     * - Timers overhead might be significant if benchmarks are small; switch to [SampleTime] mode if
     *   that is a problem.
     */
    SingleShotTime,

    /**
     * Meta-mode: all the benchmark modes.
     *
     * This is mostly useful for internal JMH testing.
     */
    All,
  }

  enum class ResultFormat(val extension: String) {
    Text("txt"),
    CSV("csv"),
    SCSV("scsv"),
    JSON("json"),
  }

  enum class ReportTimeUnit {
    Minutes,
    Seconds,
    Milliseconds,
    Microseconds,
    Nanoseconds,
  }

  enum class NativeFork {
    PerBenchmark,
    PerIteration,
  }

  enum class ProgressReporting {
    IntelliJ,
    Stdout,
  }

  enum class WarmupMode {
    /**
     * Do the individual warmup for every benchmark.
     */
    Individual,

    /**
     * Do the bulk warmup before any benchmark starts.
     */
    Bulk,

    /**
     * Do the bulk warmup before any benchmark starts,
     * and then also do individual warmups for every
     * benchmark.
     */
    BulkIndividual,
  }

  companion object {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
      prettyPrint = true
      prettyPrintIndent = "  "
    }

    fun RunnerConfiguration.encodeToJson(): String {
      val data: RunnerConfigurationData = when (this) {
        is RunnerConfigurationData    -> this
        is RunnerConfigurationBuilder -> this.build()
      }
      return json.encodeToString(RunnerConfigurationData.serializer(), data)
    }

    fun decodeFromBase64Json(content: String): RunnerConfiguration {
      @OptIn(ExperimentalEncodingApi::class)
      val decoded = Base64.decode(content)
      return decodeFromJson(decoded.decodeToString())
    }

    fun decodeFromJson(content: String): RunnerConfiguration =
      json.decodeFromString(RunnerConfigurationData.serializer(), content)
  }
}


@Serializable
internal data class RunnerConfigurationData(
  override val name: String,
  override val resultFilePath: String,
  override val progressReporting: ProgressReporting? = null,

  override val includes: List<String> = emptyList(),
  override val excludes: List<String> = emptyList(),

  override val resultFormat: ResultFormat = ResultFormat.Text,
  override val verbosity: VerboseMode? = null,
  override val failOnError: Boolean? = null,

  override val threads: Int? = null,
  override val threadGroups: List<Int> = emptyList(),

  override val enableSyncIterations: Boolean? = null,

  override val warmupForks: Int? = null,
  override val warmupMode: WarmupMode? = null,
  override val warmupIterations: Int? = null,
  override val warmupDuration: Duration? = null,
  override val warmupBatchSize: Int? = null,
  override val warmupIncludes: List<String> = emptyList(),

  override val enableGcPerIteration: Boolean? = null,
  override val measurementIterations: Int? = null,
  override val measurementDuration: Duration? = null,
  override val measurementBatchSize: Int? = null,

  override val mode: Mode? = null,
  override val resultTimeUnit: ReportTimeUnit? = null,
  override val operationsPerInvocation: Int? = null,
  override val forks: Int? = null,

  override val timeout: Duration? = null,
  override val parameters: Map<String, List<String>> = emptyMap(),

  override val jvm: String? = null,
  override val jvmArgs: List<String> = emptyList(),
  override val jvmArgsAppend: List<String> = emptyList(),
  override val jvmArgsPrepend: List<String> = emptyList(),

  override val nativeFork: NativeFork? = null,

  override val enableJsBridge: Boolean? = null,

///**
// * Profilers to use for the run.
// * Profilers will start in the order specified by collection, and will stop in the reverse order.
// * @return profilers to use; empty collection if no profilers are required
// */
//val profilers:  List<ProfilerConfig>
) : RunnerConfiguration


fun RunnerConfiguration(
  name: String,
  resultFilePath: String,
  builder: RunnerConfigurationBuilder.() -> Unit,
): RunnerConfiguration {
  return RunnerConfigurationBuilder(name = name, resultFilePath = resultFilePath)
    .apply(builder)
    .build()
}

class RunnerConfigurationBuilder internal constructor(
  override var name: String,
  override var resultFilePath: String,
) : RunnerConfiguration {
  override var progressReporting: ProgressReporting? = null
  override val includes: MutableList<String> = mutableListOf()
  override val excludes: MutableList<String> = mutableListOf()
  override var resultFormat: ResultFormat = ResultFormat.Text
  //  override var result: String? = null
  override var enableGcPerIteration: Boolean? = null
  override var verbosity: VerboseMode? = null
  override var failOnError: Boolean? = null
  override var enableJsBridge: Boolean? = null
  override var threads: Int? = null
  override val threadGroups: MutableList<Int> = mutableListOf()
  override var enableSyncIterations: Boolean? = null
  override var warmupIterations: Int? = null
  override var warmupDuration: Duration? = null
  override var warmupBatchSize: Int? = null
  override val warmupIncludes: MutableList<String> = mutableListOf()
  override var measurementIterations: Int? = null
  override var measurementDuration: Duration? = null
  override var measurementBatchSize: Int? = null
  override var mode: Mode? = null
  override var resultTimeUnit: ReportTimeUnit? = null
  override var operationsPerInvocation: Int? = null
  override var forks: Int? = null
  override var warmupForks: Int? = null
  override var jvm: String? = null
  override val jvmArgs: MutableList<String> = mutableListOf()
  override val jvmArgsAppend: MutableList<String> = mutableListOf()
  override val jvmArgsPrepend: MutableList<String> = mutableListOf()
  override var timeout: Duration? = null
  override var warmupMode: WarmupMode? = null
  override val parameters: MutableMap<String, List<String>> = mutableMapOf()
  override val nativeFork: NativeFork? = null

  internal fun build(): RunnerConfigurationData = RunnerConfigurationData(
    name = name,
//    progressReporting = progressReporting,
    includes = includes.toList(),
    excludes = excludes.toList(),
    resultFilePath = resultFilePath,
    resultFormat = resultFormat,
//    result = result,
    enableGcPerIteration = enableGcPerIteration,
    verbosity = verbosity,
    failOnError = failOnError,
    threads = threads,
    threadGroups = threadGroups.toList(),
    enableSyncIterations = enableSyncIterations,
    warmupIterations = warmupIterations,
    warmupDuration = warmupDuration,
    warmupBatchSize = warmupBatchSize,
    warmupIncludes = warmupIncludes.toList(),
    measurementIterations = measurementIterations,
    measurementDuration = measurementDuration,
    measurementBatchSize = measurementBatchSize,
    mode = mode,
    resultTimeUnit = resultTimeUnit,
    operationsPerInvocation = operationsPerInvocation,
    forks = forks,
    warmupForks = warmupForks,
    jvm = jvm,
    jvmArgs = jvmArgs.toList(),
    jvmArgsAppend = jvmArgsAppend.toList(),
    jvmArgsPrepend = jvmArgsPrepend.toList(),
    enableJsBridge = enableJsBridge,
    timeout = timeout,
    warmupMode = warmupMode,
    parameters = parameters.toMap(),
    nativeFork = nativeFork,
    progressReporting = progressReporting,
  )
}
