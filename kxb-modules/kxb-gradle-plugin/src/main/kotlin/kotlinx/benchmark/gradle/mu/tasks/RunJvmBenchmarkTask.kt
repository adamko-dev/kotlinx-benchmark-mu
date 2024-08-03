package kotlinx.benchmark.gradle.mu.tasks

import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkRunSpec
import kotlinx.benchmark.gradle.mu.internal.utils.adding
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.newInstance

@CacheableTask
abstract class RunJvmBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
  objects: ObjectFactory,
) : KxbBaseTask() {

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Nested
  val benchmarkParameters: BenchmarkRunSpec =
    extensions.adding("benchmarkParameters", objects.newInstance("test1"))

  @KotlinxBenchmarkPluginInternalApi
  @get:Input
  abstract val mainClass: Property<String>

  @KotlinxBenchmarkPluginInternalApi
  @get:Input
  @get:Optional
  abstract val ideaActive: Property<Boolean>

//  @get:Nested
//  abstract val javaLauncher: Property<JavaLauncher>

  @TaskAction
  fun action() {
    val reportFile = temporaryDir.resolve("report.html")

    val encodedParameters = encodeParameters(
      name = benchmarkParameters.name,
      reportFile = reportFile,
      traceFormat = if (ideaActive.orNull == true) "xml" else "text",
      benchmarkParameters,
    )

    val parametersFile = temporaryDir.resolve("parameters.txt").apply {
      writeText(encodedParameters)
    }

    exec.javaexec {
      mainClass = this@RunJvmBenchmarkTask.mainClass
      classpath(runtimeClasspath)
//      javaLauncher = this@ExecJvmBenchmarkTask.javaLauncher
      args(parametersFile.invariantSeparatorsPath)
    }
  }
}


private fun encodeParameters(
  name: String,
  reportFile: File,
  traceFormat: String,
  config: BenchmarkRunSpec
): String {
  validateConfig(config)
//  val file = Files.createTempFile("benchmarks", "txt").toFile()
  return buildString {
    appendLine("name:$name")
    appendLine("reportFile:$reportFile")
    appendLine("traceFormat:$traceFormat")
    config.reportFormat.orNull?.let { appendLine("reportFormat:${it.format}") }
    config.iterations.orNull?.let { appendLine("iterations:$it") }
    config.warmups.orNull?.let { appendLine("warmups:$it") }
    config.iterationDuration.orNull?.let {
      appendLine("iterationTime:${it.inWholeMilliseconds}")
      appendLine("iterationTimeUnit:${DurationUnit.MILLISECONDS}")
    }
//    config.iterationTime.orNull?.let { appendLine("iterationTime:$it") }
//    config.iterationTimeUnit.orNull?.let { appendLine("iterationTimeUnit:$it") }
    config.reportTimeUnit.orNull?.let { appendLine("outputTimeUnit:${it.unit}") }

    config.mode.orNull?.let { appendLine("mode:${it.id}") }

    config.includes.orNull?.forEach {
      appendLine("include:$it")
    }
    config.excludes.orNull?.forEach {
      appendLine("exclude:$it")
    }
    config.params.orNull?.forEach { (param, values) ->
      values.forEach { value ->
        appendLine("param:$param=$value")
      }
    }
    config.advanced.orNull?.forEach { (param, value) ->
      appendLine("advanced:$param=$value")
    }
  }
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

  config.params.orNull?.forEach { (param, values) ->
    require(param.isNotBlank()) {
      "Invalid param name: '$param'. It must not be blank."
    }
    require(values.isNotEmpty()) {
      "Param '$param' has no values. At least one value is required."
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
