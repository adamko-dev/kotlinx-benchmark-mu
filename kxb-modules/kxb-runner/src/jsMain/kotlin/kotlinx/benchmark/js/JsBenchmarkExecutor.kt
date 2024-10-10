package kotlinx.benchmark.js

import kotlin.js.Promise
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class JsBenchmarkExecutor(
  name: String,
  @Suppress("UNUSED_PARAMETER")
  dummyArgs: Array<out String>,
) :
  SuiteExecutor(
    executionName = name,
    //configPath = jsEngineSupport.arguments().first(),
    encodedBenchmarkParameters = jsEngineSupport.arguments().first(),
  ) {

  init {
    check(!isD8) { "${JsBenchmarkExecutor::class.simpleName} does not support d8 engine" }
  }

  private val benchmarkJs: BenchmarkJs = require("benchmark").unsafeCast<BenchmarkJs>()

  override fun run(
    runnerConfiguration: RunnerConfiguration,
    benchmarks: List<BenchmarkDescriptor<Any?>>,
    start: () -> Unit,
    complete: () -> Unit
  ) {
    start()
    val jsSuite = benchmarkJs.Suite()
    jsSuite.on("complete") {
      complete()
    }

    benchmarks.forEach { benchmark ->
      val suite = benchmark.suite
      val config = BenchmarkConfiguration(runnerConfiguration, suite)
      val isAsync = benchmark.isAsync

      runWithParameters(suite.parameters, runnerConfiguration.parameters, suite.defaultParameters) { params ->
        val id = id(benchmark.name, params)

        val instance = suite.factory() // TODO: should we create instance per bench or per suite?
        suite.parametrize(instance, params)

        if (isAsync) {
          when (benchmark) {
            // Mind asDynamic: this is **not** a regular promise
            is JsBenchmarkDescriptorWithNoBlackholeParameter -> {
              @Suppress("UNCHECKED_CAST")
              val promiseFunction = benchmark.function as Any?.() -> Promise<*>
              jsSuite.add(benchmark.name) { deferred: Promise<Unit> ->
                instance.promiseFunction().then { (deferred.asDynamic()).resolve() }
              }
            }

            is JsBenchmarkDescriptorWithBlackholeParameter   -> {
              @Suppress("UNCHECKED_CAST")
              val promiseFunction = benchmark.function as Any?.(Blackhole) -> Promise<*>
              jsSuite.add(benchmark.name) { deferred: Promise<Unit> ->
                instance.promiseFunction(benchmark.blackhole).then { (deferred.asDynamic()).resolve() }
              }
            }

            else                                             ->
              error("Unexpected benchmark descriptor ${benchmark::class.simpleName}")
          }
        } else {
          when (benchmark) {
            is JsBenchmarkDescriptorWithNoBlackholeParameter -> {
              val function: Any?.() -> Any? = benchmark.function
              jsSuite.add(benchmark.name) { instance.function() }
            }

            is JsBenchmarkDescriptorWithBlackholeParameter   -> {
              val function: Any?.(Blackhole) -> Any? = benchmark.function
              jsSuite.add(benchmark.name) { instance.function(benchmark.blackhole) }
            }

            else                                             ->
              error("Unexpected benchmark descriptor ${benchmark::class.simpleName}")
          }
        }

        val jsBenchmark = jsSuite[jsSuite.length - 1] // take back last added benchmark and subscribe to events

        // TODO: Configure properly
        // initCount: The default number of times to execute a test on a benchmark’s first cycle
        // minTime: The time needed to reduce the percent uncertainty of measurement to 1% (secs).
        // maxTime: The maximum time a benchmark is allowed to run before finishing (secs).

        jsBenchmark.options.initCount = config.warmups
        jsBenchmark.options.minSamples = config.iterations
//        val iterationSeconds = config.iterationTime * config.iterationTimeUnit.toSecondsMultiplier()
        val iterationSeconds = config.measurementDuration.toDouble(SECONDS)
        jsBenchmark.options.minTime = iterationSeconds
        jsBenchmark.options.maxTime = iterationSeconds
        jsBenchmark.options.async = isAsync
        jsBenchmark.options.defer = isAsync

        jsBenchmark.on("start") { _ ->
          reporter.startBenchmark(executionName, id)
          suite.setup(instance)
        }
        var iteration = 0
        jsBenchmark.on("cycle") { event: dynamic ->
          val target = event.target
          val nanos = (target.times.period as Double).seconds.toDouble(NANOSECONDS)
          val sample = nanos.nanosToText(config.mode, config.outputTimeUnit)
          // (${target.cycles} × ${target.count} calls) -- TODO: what's this?
          reporter.output(
            suite = executionName,
            benchmark = id,
            message = "Iteration #${iteration++}: $sample",
          )
        }
        jsBenchmark.on("complete") { event: dynamic ->
          suite.teardown(instance)
          benchmark.blackhole.flush()
          val stats = event.target.stats
          val samples = stats.sample
            .unsafeCast<DoubleArray>()
            .map {
              val nanos = it.seconds.inWholeNanoseconds.toDouble()
              nanos.nanosToSample(config.mode, config.outputTimeUnit)
            }
            .toDoubleArray()
          val result = ReportBenchmarksStatistics.createResult(benchmark, params, config, samples)
          val message = buildString {
            append("  ~ ")
            append(result.score.sampleToText(config.mode, config.outputTimeUnit))
            append(" ±")
            append((result.error / result.score * 100).formatSignificant(2))
            append("%")
          }
          val error = event.target.error
          if (error == null) {
            reporter.endBenchmark(
              executionName,
              id,
              BenchmarkProgress.FinishStatus.Success,
              message
            )
            result(result)
          } else {
            val stacktrace = error.stack
            reporter.endBenchmarkException(
              executionName,
              id,
              error.toString(),
              stacktrace.toString()
            )
          }
        }
        Unit
      }
    }
    jsSuite.run()
  }
}


private external interface BenchmarkJs {
  fun Suite(): Suite
}

private external interface Suite {
  fun on(s: String, function: (event: dynamic) -> Unit)
  fun add(name: String, function: (event: dynamic) -> Any?)
  fun run()
  val length: Int
}

private operator fun Suite.get(index: Int): Benchmark =
  asDynamic()[index].unsafeCast<Benchmark>()

private external interface Benchmark {
  val name: String
  val options: BenchmarkJsOptions
  fun on(s: String, function: (event: dynamic) -> Unit)
}


fun main() {
  val integerValue: Long = 214000000
  Long.MAX_VALUE

}

private external interface BenchmarkJsOptions {

  /** A flag to indicate that benchmark cycles will execute asynchronously by default. */
  var async: Boolean?

  /** A flag to indicate that the benchmark clock is deferred. */
  var defer: Boolean?

  /** The delay between test cycles (secs). */
  var delay: Double?

  /** Displayed by [Benchmark.toString] when a [name] is not available (auto-generated if absent). */
  var id: String?

  /** The default number of times to execute a test on a benchmark's first cycle. */
  var initCount: Int?

  /**
   * The maximum time a benchmark is allowed to run before finishing (secs).
   *
   * Note: Cycle delays aren't counted toward the maximum time.
   */
  var maxTime: Double?

  /** The minimum sample size required to perform statistical analysis. */
  var minSamples: Int?

  /** The time needed to reduce the percent uncertainty of measurement to 1% (secs). */
  var minTime: Double?

  /** The name of the benchmark. */
  var name: String

  /** An event listener called when the benchmark is aborted. */
  var onAbort: (() -> Unit)?

  /** An event listener called when the benchmark completes running. */
  var onComplete: (() -> Unit)?

  /** An event listener called after each run cycle. */
  var onCycle: (() -> Unit)?

  /** An event listener called when a test errors. */
  var onError: (() -> Unit)?

  /** An event listener called when the benchmark is reset. */
  var onReset: (() -> Unit)?

  /** An event listener called when the benchmark starts running. */
  var onStart: (() -> Unit)?
}
