package kotlinx.benchmark.jvm

import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlinx.benchmark.BenchmarkProgress
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.RunnerConfiguration.Mode.*
import kotlinx.benchmark.RunnerConfiguration.ProgressReporting
import kotlinx.benchmark.RunnerConfiguration.ReportTimeUnit.*
import kotlinx.benchmark.RunnerConfiguration.ResultFormat.*
import kotlinx.benchmark.VerboseMode
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.readFile
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.results.format.ResultFormatFactory
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import org.openjdk.jmh.runner.options.WarmupMode


@KotlinxBenchmarkRuntimeInternalApi
fun main(args: Array<String>) {
  val config = RunnerConfiguration.decodeFromJson(args[0].readFile())
  runJvmBenchmark(config)
}

@KotlinxBenchmarkRuntimeInternalApi
fun runJvmBenchmark(
  config: RunnerConfiguration,
  demoMode: Boolean = false,
) {
  val jmhOptions = OptionsBuilder().apply {
    result(config.resultFilePath)

    resultFormat(
      when (config.resultFormat) {
        Text -> ResultFormatType.TEXT
        CSV  -> ResultFormatType.CSV
        SCSV -> ResultFormatType.SCSV
        JSON -> ResultFormatType.JSON
      }
    )

    config.measurementIterations?.let { measurementIterations(it) }

    includes += config.includes
    excludes += config.excludes

    config.enableGcPerIteration?.let { shouldDoGC(it) }

    config.verbosity?.let {
      verbosity(
        when (it) {
          VerboseMode.Silent -> org.openjdk.jmh.runner.options.VerboseMode.SILENT
          VerboseMode.Normal -> org.openjdk.jmh.runner.options.VerboseMode.NORMAL
          VerboseMode.Extra  -> org.openjdk.jmh.runner.options.VerboseMode.EXTRA
        }
      )
    }
    config.failOnError?.let { shouldFailOnError(it) }
    config.threads?.let { threads(it) }
    if (config.threadGroups.isNotEmpty()) {
      threadGroups(*config.threadGroups.toIntArray())
    }
    config.enableSyncIterations?.let { syncIterations(it) }
    config.warmupIterations?.let { warmupIterations(it) }
    config.warmupDuration?.let { warmupTime(it.toTimeValue()) }
    config.warmupBatchSize?.let { warmupBatchSize(it) }
    warmupIncludes += config.warmupIncludes
    config.measurementIterations?.let { measurementIterations(it) }
    config.measurementDuration?.let { measurementTime(it.toTimeValue()) }
    config.measurementBatchSize?.let { measurementBatchSize(it) }
    config.mode?.let {
      mode(
        when (it) {
          Throughput     -> Mode.Throughput
          AverageTime    -> Mode.AverageTime
          SampleTime     -> Mode.SampleTime
          SingleShotTime -> Mode.SingleShotTime
          All            -> Mode.All
        }
      )
    }
    config.resultTimeUnit?.let {
      timeUnit(
        when (it) {
          Minutes      -> TimeUnit.MINUTES
          Seconds      -> TimeUnit.SECONDS
          Milliseconds -> TimeUnit.MILLISECONDS
          Microseconds -> TimeUnit.MICROSECONDS
          Nanoseconds  -> TimeUnit.NANOSECONDS
        }
      )
    }
    config.warmupMode?.let {
      warmupMode(
        when (it) {
          RunnerConfiguration.WarmupMode.Individual     -> WarmupMode.INDI
          RunnerConfiguration.WarmupMode.Bulk           -> WarmupMode.BULK
          RunnerConfiguration.WarmupMode.BulkIndividual -> WarmupMode.BULK_INDI
        }
      )
    }
    config.operationsPerInvocation?.let { operationsPerInvocation(it) }
    config.forks?.let { forks(it) }
    config.warmupForks?.let { warmupForks(it) }
    config.jvm?.let { jvm(it) }
    jvmArgs(*config.jvmArgs.toTypedArray())
    jvmArgsAppend(*config.jvmArgsAppend.toTypedArray())
    jvmArgsPrepend(*config.jvmArgsPrepend.toTypedArray())
    config.timeout?.let { timeout(it.toTimeValue()) }
  }

  config.parameters.forEach { (key, value) ->
    jmhOptions.param(key, *value.toTypedArray())
  }

  if (demoMode) {
    jmhOptions
      .shouldFailOnError(false)
      .measurementTime(TimeValue.milliseconds(1))
      .warmupTime(TimeValue.milliseconds(1))
      .forks(1)
      .shouldDoGC(false)
      .syncIterations(false)
      .timeout(TimeValue.seconds(1))
  }


//  val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
//  val jvmArgs = runtimeMXBean.inputArguments
//  if (jvmArgs.any { it.contains("libasyncProfiler") }) {
//    jmhOptions.forks(0)
//  } else {
//    when (val jvmForks = config.advanced["jvmForks"]) {
//      null           -> jmhOptions.forks(1)
//      "definedByJmh" -> { /* do not override */
//      }
//
//      else           -> {
//        val forks = jvmForks.toIntOrNull()?.takeIf { it >= 0 }
//          ?: throw IllegalArgumentException("jvmForks: expected a non-negative integer or \"definedByJmh\" string literal")
//        jmhOptions.forks(forks)
//      }
//    }
//  }

//    jmhOptions.jvmArgs(
//        "-Djmh.executor=CUSTOM",
//        "-Djmh.executor.class=kotlinx.benchmark.jvm.NoopExecutorService",
//    )
//
//    System.setProperty("jmh.executor", "CUSTOM")
//    System.setProperty("jmh.executor.class", "kotlinx.benchmark.jvm.NoopExecutorService")
//    ResultFormatFactory.getInstance(
//        jmhOptions.resultFormat.get(),
//        "file"
//    )
//    BenchmarkList.fromString("")
//    CompilerHints.getCompileCommandFiles(listOf(""))

  val reportFormat = when (config.resultFormat) {
    Text -> ResultFormatType.TEXT
    CSV  -> ResultFormatType.CSV
    SCSV -> ResultFormatType.SCSV
    JSON -> ResultFormatType.JSON
//    null -> ResultFormatType.TEXT
  }
  val reporter = BenchmarkProgress.create(config.progressReporting ?: ProgressReporting.Stdout)
  val output = JmhOutputFormat(reporter, config.name)
  try {
    val runner = Runner(jmhOptions.build(), output)
    runner.list()
    val results = runner.run()
    val resultFormat = ResultFormatFactory.getInstance(reportFormat, PrintStream(File(config.resultFilePath)))
    resultFormat.writeOut(results)
  } catch (e: Exception) {
    e.printStackTrace()
    reporter.endBenchmark(
      config.name,
      output.lastBenchmarkStart,
      BenchmarkProgress.FinishStatus.Failure,
      e.message ?: "<unknown error>"
    )
  }
}

/*
private fun Collection<RunResult>.toReportBenchmarkResult(): Collection<ReportBenchmarkResult> = map { result ->
    val benchmarkFQN = result.params.benchmark
    val value = result.primaryResult

    val (min, max) = value.getScoreConfidence()
    val statistics = value.getStatistics()
    val percentiles = listOf(0.0, 0.25, 0.5, 0.75, 0.90, 0.99, 0.999, 0.9999, 1.0).associate {
        (it * 100) to statistics.getPercentile(it)
    }

    val rawData = result.benchmarkResults
        .flatMap { run -> run.iterationResults.map { iteration -> iteration.primaryResult.getScore() } }
        .toDoubleArray()

    ReportBenchmarkResult(benchmarkFQN, value.getScore(), value.getScoreError(), min to max, percentiles, rawData)
}
*/


private fun Duration.toTimeValue(): TimeValue =
  toComponents { days, hours, minutes, seconds, nanoseconds ->
    return when {
      nanoseconds > 0 -> TimeValue.milliseconds(inWholeMilliseconds) // millis are good enough, nanos might be too small
      seconds > 0     -> TimeValue.seconds(inWholeSeconds)
      minutes > 0     -> TimeValue.minutes(inWholeMinutes)
      hours > 0       -> TimeValue.hours(inWholeHours)
      days > 0        -> TimeValue.days(inWholeDays)
      else            -> TimeValue.milliseconds(inWholeMilliseconds)
    }
  }
