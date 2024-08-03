package kotlinx.benchmark.gradle.mu.config.jmh

import kotlin.time.Duration
import kotlinx.benchmark.gradle.mu.config.jmh.*
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional


interface JmhOptionsSpec {
  /** Benchmarks to run. */
  @get:Input
  @get:Optional
  val includes: SetProperty<String>

  /** Benchmarks to exclude from the run. */
  @get:Input
  @get:Optional
  val excludes: SetProperty<String>

  /**
   * Number of measurement iterations to do.
   *
   * Measurement iterations are counted towards the benchmark score.
   */
  @get:Input
  @get:Optional
  val iterations: Property<Int>

  /**
   * Batch size: number of benchmark method calls per operation.
   * Some benchmark modes may ignore this setting, please check this separately.
   */
  @get:Input
  @get:Optional
  val batchSize: Property<Int>

  /**
   * Minimum time to spend at each measurement iteration.
   * Benchmarks may generally run longer than iteration duration.
   */
  @get:Input
  @get:Optional
  val runTime: Property<Duration>


  //region warmup

  /** Warmup mode for warming up selected benchmarks. */
  @get:Input
  @get:Optional
  val warmupMode: Property<WarmupMode>

  /**
   * Warmup benchmarks to include in the run, in addition to those selected by the primary filters.
   * Harness will not measure these benchmarks, but only use them for the warmup.
   */
  @get:Input
  @get:Optional
  val warmupBenchmarks: SetProperty<String>

  /**
   * Number of warmup iterations to do.
   * Warmup iterations are not counted towards the benchmark score.
   */
  @get:Input
  @get:Optional
  val warmupIterations: Property<Int>

  /**
   * Warmup batch size: number of benchmark method calls per operation.
   * Some benchmark modes may ignore this setting.
   */
  @get:Input
  @get:Optional
  val warmupBatchSize: Property<Int>

  /**
   * How many warmup forks to make for a single benchmark.
   *
   * All iterations within the warmup fork are not counted towards the benchmark score.
   *
   * Use 0 to disable warmup forks.
   */
  @get:Input
  @get:Optional
  val warmupForks: Property<Int>

  /**
   * Minimum time to spend at each warmup iteration.
   * Benchmarks may generally run longer than iteration duration.
   */
  @get:Input
  @get:Optional
  val warmupTime: Property<Duration>

  //endregion

  /**
   * Timeout for benchmark iteration.
   * After reaching this timeout, JMH will try to interrupt the running tasks.
   * Non-cooperating benchmarks may ignore this timeout.
   */
  @get:Input
  @get:Optional
  val timeout: Property<Duration>

  /**
   * Number of worker threads to run with.
   * 'max' means the maximum number of hardware threads available on the machine, figured out by JMH itself.
   */
  @get:Input
  @get:Optional
  val threads: Property<Int>

  /**
   * Should JMH synchronize iterations?
   *
   * This would significantly lower the noise in multithreaded tests,
   * by making sure the measured part happens only when all workers are running.
   */
  @get:Input
  @get:Optional
  val synchronizeIterations: Property<Boolean>

  /**
   * Should JMH force GC between iterations?
   *
   * Forcing the GC may help to lower the noise in GC-heavy benchmarks,
   * at the expense of jeopardizing GC ergonomics decisions.
   *
   * Use with care.
   */
  @get:Input
  @get:Optional
  val gcEachIteration: Property<Boolean>

  /**
   * Should JMH fail immediately if any benchmark had experienced an unrecoverable error?
   *
   * This helps to make quick sanity tests for benchmark suites,
   * as well as make the automated runs with checking error codes.
   */
  @get:Input
  @get:Optional
  val failOnError: Property<Boolean>

  /**
   * How many times to fork a single benchmark.
   *
   * Use 0 to disable forking altogether.
   *
   * Warning: disabling forking may have detrimental impact on benchmark and infrastructure reliability,
   * you might want to use different warmup mode instead.
   */
  @get:Input
  @get:Optional
  val fork: Property<Int>

  /**
   * Override thread group distribution for asymmetric benchmarks.
   *
   * See
   * [org.openjdk.jmh.annotations.Group] and [org.openjdk.jmh.annotations.GroupThreads]
   * for more information.
   */
  @get:Input
  @get:Optional
//   * This option expects a comma-separated list of thread counts within the group.
  val threadGroups: ListProperty<Int>

  /**
   * Override operations per invocation, see
   * [org.openjdk.jmh.annotations.OperationsPerInvocation] Javadoc for details.
   */
  @get:Input
  @get:Optional
  val opsPerInvocation: Property<Int>

  /** Override time unit in benchmark results */
  @get:Input
  @get:Optional
  val resultTimeUnit: Property<ReportTimeUnit>

  /** Benchmark mode. */
  @get:Input
  @get:Optional
  val mode: Property<BenchmarkMode>

  /** Verbosity mode. */
  @get:Input
  @get:Optional
  val verbosity: Property<VerboseMode>

  /**
   * Use profilers to collect additional benchmark data.
   * Some profilers are not available on all JVMs and/or all OSes.
   */
  // TODO collect list of profilers: "Please see the list of available profilers with -lprof."
  @get:Input
  @get:Optional
  val profilers: SetProperty<String>


  /** Format type for machine-readable results. These results are written to a separate file (see -rff). */
  // See the list of available result formats with -lrf.
  @get:Input
  @get:Optional
  val resultFormat: Property<ResultFormat>


  /** Benchmark parameters. This option is expected to be used once per parameter. */
  //  Parameter name and parameter values should be separated with equals sign. Parameter values should be separated with commas.
  @get:Input
  @get:Optional
  val parameters: NamedDomainObjectContainer<BenchmarkParameter>

  /**
   * Use given JVM arguments.
   *
   * Most options are inherited from the host VM options, but in some cases you want to
   * pass the options only to a forked VM.
   *
   * Either single space-separated option line or multiple options are accepted.
   *
   * This option only affects forked runs.
   */
  @get:Input
  @get:Optional
  val jvmArgs: ListProperty<String>

//  /** Same as [jvmArgs], but append these options after the already given JVM args. */
//  @get:Input
//  @get:Optional
//  val jvmArgsAppend: ListProperty<String>
//
//  /** Same as [jvmArgs], but prepend these options after the already given JVM args. */
//  @get:Input
//  @get:Optional
//  val jvmArgsPrepend: ListProperty<String>
}


//OptionSpec<String> optOutput = parser.accepts("o", "Redirect human-readable output to a given file.")
//.withRequiredArg().ofType(String.class).describedAs("filename");

//OptionSpec<String> optOutputResults = parser.accepts("rff", "Write machine-readable results to a given file. " +
//"The file format is controlled by -rf option. Please see the list of result formats for available " +
//"formats. " +
//"(default: " + Defaults.RESULT_FILE_PREFIX + ".<result-format>)")
//.withRequiredArg().ofType(String.class).describedAs("filename");


//parser.accepts("l", "List the benchmarks that match a filter, and exit.");
//parser.accepts("lp", "List the benchmarks that match a filter, along with parameters, and exit.");
//parser.accepts("lrf", "List machine-readable result formats, and exit.");
//parser.accepts("lprof", "List profilers, and exit.");
//parser.accepts("h", "Display help, and exit.");


//OptionSpec<String> optJvm = parser.accepts("jvm", "Use given JVM for runs. This option only affects forked runs.")
//.withRequiredArg().ofType(String.class).describedAs("string");
