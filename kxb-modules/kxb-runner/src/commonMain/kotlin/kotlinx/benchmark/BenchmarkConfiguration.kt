package kotlinx.benchmark

import kotlin.time.Duration
import kotlinx.benchmark.RunnerConfiguration.Mode.*
import kotlinx.benchmark.RunnerConfiguration.ReportTimeUnit.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.serialization.Serializable

@KotlinxBenchmarkRuntimeInternalApi
@Serializable
data class BenchmarkConfiguration(
  val iterations: Int,
  val warmups: Int,
  val measurementDuration: Duration,
//  val iterationTime: Long,
//  val iterationTimeUnit: BenchmarkTimeUnit,
  val outputTimeUnit: BenchmarkTimeUnit,
  val mode: Mode,
//  val advanced: Map<String, String>,
  val enableJsBridge: Boolean,
  val nativeFork: RunnerConfiguration.NativeFork?,
  val enableGcPerIteration: Boolean,
) {

  constructor(
    runner: RunnerConfiguration,
    suite: SuiteDescriptor<*>,
  ) : this(
    iterations = runner.measurementIterations ?: suite.iterations,
    warmups = runner.warmupIterations ?: suite.warmups,
    measurementDuration = runner.measurementDuration ?: suite.measurementDuration,
//    iterationTime = runner.measurementDuration?.inWholeMilliseconds ?: suite.iterationTime.value,
//    iterationTimeUnit =
//    if (runner.measurementDuration == null) BenchmarkTimeUnit.MILLISECONDS else suite.iterationTime.timeUnit,
    outputTimeUnit = runner.resultTimeUnit?.convert() ?: suite.outputTimeUnit,
    mode = runner.mode?.convert() ?: suite.mode,
    enableJsBridge = runner.enableJsBridge ?: false,
    nativeFork = runner.nativeFork ?: RunnerConfiguration.NativeFork.PerBenchmark,
    enableGcPerIteration = runner.enableGcPerIteration ?: false,
//    advanced = runner.advanced
  )

  companion object {


//    override fun toString() =
//        "iterations=$iterations, warmups=$warmups, iterationTime=$iterationTime, " +
//                "iterationTimeUnit=${iterationTimeUnit.toText()}, outputTimeUnit=${outputTimeUnit.toText()}, " +
//                "mode=${mode.toText()}" +
//                advanced.entries.joinToString(prefix = ", ", separator = ", ") { "advanced:${it.key}=${it.value}" }

//    @KotlinxBenchmarkRuntimeInternalApi
//    companion object {
//        fun parse(description: String): BenchmarkConfiguration {
//            val parameters = description.parseMap()
//            fun getParameterValue(key: String) =
//                parameters[key] ?: throw NoSuchElementException("Parameter `$key` is required.")
//
//            val advanced = parameters
//                .filter { it.key.startsWith("advanced:") }
//                .entries
//                .associate {
//                    val advancedKey = it.key.substringAfter(":")
//                    check(advancedKey.isNotEmpty()) { "Invalid advanced key - should not be empty" }
//                    advancedKey to it.value
//                }
//
//            return BenchmarkConfiguration(
//                iterations = getParameterValue("iterations").toInt(),
//                warmups = getParameterValue("warmups").toInt(),
//                iterationTime = getParameterValue("iterationTime").toLong(),
//                iterationTimeUnit = parseTimeUnit(getParameterValue("iterationTimeUnit")),
//                outputTimeUnit = parseTimeUnit(getParameterValue("outputTimeUnit")),
//                mode = getParameterValue("mode").toMode(),
//                advanced = advanced
//            )
//        }
  }
}

//internal fun String.parseMap(): Map<String, String> =
//    removeSurrounding("{", "}")
//        .split(", ")
//        .filter { it.isNotEmpty() }
//        .associate {
//            val keyValue = it.split("=")
//            require(keyValue.size == 2) { "Wrong format of map string description!" }
//            val (key, value) = keyValue
//            key to value
//        }

private fun RunnerConfiguration.ReportTimeUnit.convert(): BenchmarkTimeUnit {
  return when (this) {
    Minutes      -> BenchmarkTimeUnit.MINUTES
    Seconds      -> BenchmarkTimeUnit.SECONDS
    Milliseconds -> BenchmarkTimeUnit.MILLISECONDS
    Microseconds -> BenchmarkTimeUnit.MICROSECONDS
    Nanoseconds  -> BenchmarkTimeUnit.NANOSECONDS
  }
}

private fun RunnerConfiguration.Mode.convert(): Mode {
  return when (this) {
    Throughput     -> Mode.Throughput
    AverageTime    -> Mode.AverageTime
    SampleTime     -> Mode.SampleTime
    SingleShotTime -> Mode.SingleShotTime
    All            -> Mode.All
  }
}
