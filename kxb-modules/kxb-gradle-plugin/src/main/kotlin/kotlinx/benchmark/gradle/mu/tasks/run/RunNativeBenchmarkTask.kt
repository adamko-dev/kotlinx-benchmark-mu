package kotlinx.benchmark.gradle.mu.tasks.run

import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.*
import kotlinx.benchmark.gradle.absolutePath
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget.Kotlin.Native.ForkMode
import kotlinx.benchmark.gradle.mu.tasks.run.NativeBenchmarkOperation.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
abstract class RunNativeBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseRunBenchmarksTask() {

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val executable: RegularFileProperty

//  @get:InputFiles
//  @get:PathSensitive(RELATIVE)
//  abstract val executable: ConfigurableFileCollection

  @get:Internal
  abstract val workingDir: DirectoryProperty

//  @get:InputFile
//  @get:PathSensitive(RELATIVE)
//  abstract val configFile: RegularFileProperty

  @get:Input
  @get:Optional
  abstract val forkMode: Property<ForkMode>

  @get:Internal
  abstract val benchmarkDescriptionDir: DirectoryProperty

  @get:Internal
  abstract val benchmarkProgress: RegularFileProperty

  @TaskAction
  protected fun run() {
    workingDir.get().asFile.mkdirs()
    benchmarkDescriptionDir.get().asFile.mkdirs()
    benchmarkProgress.get().asFile.apply {
      parentFile.mkdirs()
      createNewFile()
    }

    runOld()
  }
//  private fun knBinary(): File = executable.singleOrNull()
//    ?: error("[$path] missing Kotlin/Native binary")

  private fun execute(
//    configFile: File,
    encodedBenchmarkParameters: String,
    operation: NativeBenchmarkOperation,
    args: List<String>,
  ) {

    val allArgs = buildList {
      add(encodedBenchmarkParameters)
      add(operation.flag)
      addAll(args)
    }

    try {
      exec.exec {
        this.executable(this@RunNativeBenchmarkTask.executable.get().asFile)
//        this.executable(this@RunNativeBenchmarkTask.knBinary())
        this.args(allArgs)
        this.workingDir(this@RunNativeBenchmarkTask.workingDir.get().asFile)
        this.standardOutput = System.out
        this.errorOutput = System.err
      }
    } catch (ex: Exception) {
      logger.error("[$path] failed to run ${executable.get().asFile}, allArgs: $allArgs")
      throw ex
    }
  }

  private fun runOld() {
    val encodedBenchmarkParameters = encodeBenchmarkParameters()

//    val configFile = configFile.get().asFile
    val benchmarkProgressPath = benchmarkProgress.get().asFile.absolutePath
    val benchmarkDescriptionDir = benchmarkDescriptionDir.get().asFile

    // Get full list of running benchmarks
    execute(
      encodedBenchmarkParameters,
      ListRunning,
      listOf(benchmarkProgressPath, benchmarkDescriptionDir.absolutePath)
    )
    val detailedConfigFiles = objects.fileTree().from(benchmarkDescriptionDir).files.sortedBy { it.absolutePath }
    val runResults = mutableMapOf<String, String>()

    val forkPerBenchmark = forkMode.orNull.let { it == null || it == ForkMode.PerBenchmark }

    detailedConfigFiles.forEach { runConfig ->
      val runConfigPath = runConfig.absolutePath
      val lines = runConfig.readLines()
      require(lines.size > 1) { "Wrong detailed configuration format" }
      val currentConfigDescription = lines[1]

      // Execute benchmark
      if (forkPerBenchmark) {
        val suiteResultsFile = createTempFile("bench", ".txt")
        execute(
          encodedBenchmarkParameters,
          Benchmark,
          listOf(
            benchmarkProgressPath,
            runConfigPath,
            suiteResultsFile.absolutePath
          )
        )
        val suiteResults = suiteResultsFile.readText()
        if (suiteResults.isNotEmpty())
          runResults[runConfigPath] = suiteResults
      } else {
        val iterations = currentConfigDescription.substringAfter("iterations=")
          .substringBefore(',').toInt()
        val warmups = currentConfigDescription.substringAfter("warmups=")
          .substringBefore(',').toInt()
        // Warm up
        var exceptionDuringExecution = false
        var textResult: Path? = null
        for (i in 0 until warmups) {
          textResult = createTempFile("bench", ".txt")
          execute(
            encodedBenchmarkParameters,
            Warmup,
            listOf(
              benchmarkProgressPath,
              runConfigPath,
              i.toString(),
              textResult.absolutePath
            )
          )
          val result = textResult.readLines().getOrNull(0)
          if (result == "null") {
            exceptionDuringExecution = true
            break
          }
        }
        // Get cycles number
        val cycles = if (!exceptionDuringExecution && textResult != null) textResult.readText() else "1"
        // Execution
        val iterationResults = mutableListOf<Double>()
        var iteration = 0
        while (!exceptionDuringExecution && iteration in 0 until iterations) {
          textResult = createTempFile("bench", ".txt")
          execute(
            encodedBenchmarkParameters,
            Iteration,
            listOf(
              benchmarkProgressPath,
              runConfigPath,
              iteration.toString(),
              cycles,
              textResult.absolutePath
            )
          )
          val result = textResult.readLines()[0]
          if (result == "null")
            exceptionDuringExecution = true
          iterationResults.add(result.toDouble())
          iteration++
        }
        // Store results
        if (iterationResults.size == iterations) {
          val iterationsResultsFile = createTempFile("bench_results")
          iterationsResultsFile.bufferedWriter().use { out ->
            out.write(iterationResults.joinToString { it.toString() })
          }
          execute(
            encodedBenchmarkParameters,
            EndRun,
            listOf(
              benchmarkProgressPath,
              runConfigPath,
              iterationsResultsFile.absolutePath
            )
          )
          runResults[runConfigPath] = iterationResults.joinToString()
        }
      }
    }
    // Merge results
    val samplesFile = createTempFile("bench_results")
    samplesFile.writeText(
      runResults.entries.joinToString("\n") { (k, v) -> "$k: $v" }
    )
    execute(
      encodedBenchmarkParameters,
      StoreResults,
      listOf(benchmarkProgressPath, samplesFile.absolutePath)
    )
  }
}

private enum class NativeBenchmarkOperation(val flag: String) {
  EndRun("--end-run"),
  StoreResults("--store-results"),
  Warmup("--warmup"),
  Iteration("--iteration"),
  Benchmark("--benchmark"),
  ListRunning("--list"),
  ;
}

//
//  //  @TaskAction
////  protected fun run() {
////    val configFile = configFile.get().asFile
////    val benchmarkProgressPath = benchmarkProgressPath.get().asFile
////    val benchmarkDescriptionDir = benchmarkDescriptionDir.get().asFile
////    val forkMode: ForkMode = forkMode.get()
////
////    // Get the full list of running benchmarks
////    val detailedConfigFiles: List<File> =
////      listActiveBenchmarks(configFile, benchmarkProgressPath, benchmarkDescriptionDir)
////
////    val runResults: MutableMap<String, String> = mutableMapOf()
////
////    detailedConfigFiles.forEach { runConfig ->
////      executeBenchmark(
////        forkMode = forkMode,
////        configFile = configFile,
////        benchmarkProgressPath = benchmarkProgressPath.absolutePath,
////        runConfigPath = runConfig.absolutePath,
////        runResults = runResults
////      )
////    }
////
////    detailedConfigFiles.forEach { runConfig ->
////      val runConfigPath = runConfig.absolutePath
////      val lines = runConfig.readLines()
////      require(lines.size > 1) { "Wrong detailed configuration format" }
////      val currentConfigDescription = lines[1]
////
////      // Execute benchmark
////      if (forkMode == PerBenchmark) {
////        executeForkedBenchmark(
////          configFile = configFile,
////          benchmarkProgressPath = benchmarkProgressPath.absolutePath,
////          runConfigPath = runConfigPath,
////          runResults = runResults,
////        )
////      } else {
////        runPerTestBenchmark(
////          currentConfigDescription = currentConfigDescription,
////          benchmarkProgressPath = benchmarkProgressPath,
////          configFile = configFile,
////          runConfigPath = runConfigPath,
////          runResults = runResults,
////        )
////      }
////    }
////
////    // Merge results
////    mergeResults(
////      configFile = configFile,
////      benchmarkProgressPath = benchmarkProgressPath.absolutePath,
////      runResults = runResults,
////    )
////  }
////
////  private fun executeBenchmark(
////    forkMode: ForkMode,
////    configFile: File,
////    benchmarkProgressPath: String,
////    runConfigPath: String,
////    runResults: MutableMap<String, String>,
////  ) {
////    when (forkMode) {
////      PerTest -> runPerTestBenchmark()
////      PerBenchmark -> executeForkedBenchmark(
////        configFile = configFile,
////        benchmarkProgressPath = benchmarkProgressPath,
////        runConfigPath = runConfigPath,
////        runResults = runResults,
////      )
////    }
////  }
////
////  private fun executeBenchmarks() {}
////
////  private fun executeWarmups(
////    configFile: File,
////    benchmarkProgressPath: String,
////    runConfigPath: String,
////    warmups: Int,
////  ) {
////    var exceptionDuringExecution = false
////    var textResult: Path?
////    for (i in 0 until warmups) {
////      textResult = createTempFile("bench", ".txt")
////      execute(
////        configFile,
////        Warmup,
////        listOf(
////          benchmarkProgressPath,
////          runConfigPath,
////          i.toString(),
////          textResult.absolutePath
////        )
////      )
////      val result = textResult.readLines().firstOrNull()
////      if (result == "null") {
////        exceptionDuringExecution = true
////        break
////      }
////    }
////  }
////
////  private fun runPerTestBenchmark(
////    currentConfigDescription: String,
////    benchmarkProgressPath: File,
////    configFile: File,
////    runConfigPath: String,
////    runResults: MutableMap<String, String>,
////  ) {
////
////    val iterations = currentConfigDescription.substringAfter("iterations=")
////      .substringBefore(',').toInt()
////    val warmups = currentConfigDescription.substringAfter("warmups=")
////      .substringBefore(',').toInt()
////    // Warm up
////    var exceptionDuringExecution = false
////    var textResult: Path? = null
////    for (i in 0 until warmups) {
////      textResult = createTempFile("bench", ".txt")
////      execute(
////        configFile,
////        Warmup,
////        listOf(
////          benchmarkProgressPath.absolutePath,
////          runConfigPath,
////          i.toString(),
////          textResult.absolutePath
////        )
////      )
////      val result = textResult.readLines().firstOrNull()
////      if (result == "null") {
////        exceptionDuringExecution = true
////        break
////      }
////    }
////    // Get cycles number
////    val cycles = if (!exceptionDuringExecution && textResult != null) textResult.readText() else "1"
////    // Execution
////    val iterationResults = mutableListOf<Double>()
////    var iteration = 0
////    while (!exceptionDuringExecution && iteration in 0 until iterations) {
////      textResult = createTempFile("bench", ".txt")
////      execute(
////        configFile,
////        Iteration,
////        listOf(
////          benchmarkProgressPath.absolutePath,
////          runConfigPath,
////          iteration.toString(),
////          cycles,
////          textResult.absolutePath,
////        )
////      )
////      val result = textResult.readLines()[0]
////      if (result == "null") {
////        exceptionDuringExecution = true
////      }
////      iterationResults.add(result.toDouble())
////      iteration++
////    }
////    // Store results
////    storeResults(
////      configFile = configFile,
////      iterationResults = iterationResults,
////      runResults = runResults,
////      iterations = iterations,
////      runConfigPath = runConfigPath,
////      benchmarkProgressPath = benchmarkProgressPath.absolutePath,
////    )
////  }
////
////  private fun executeForkedBenchmark(
////    configFile: File,
////    benchmarkProgressPath: String,
////    runConfigPath: String,
////    runResults: MutableMap<String, String>,
////  ) {
////    val suiteResultsFile = createTempFile("bench", ".txt")
////    execute(
////      configFile,
////      Benchmark,
////      listOf(
////        benchmarkProgressPath,
////        runConfigPath,
////        suiteResultsFile.absolutePath
////      )
////    )
////    val suiteResults = suiteResultsFile.readText()
////    if (suiteResults.isNotEmpty()) {
////      runResults[runConfigPath] = suiteResults
////    }
////  }
////
////  private fun storeResults(
////    configFile: File,
////    iterationResults: List<Double>,
////    runResults: MutableMap<String, String>,
////    iterations: Int,
////    runConfigPath: String,
////    benchmarkProgressPath: String,
////  ) {
////    if (iterationResults.size == iterations) {
////      val iterationsResultsFile = createTempFile("bench_results")
////      iterationsResultsFile.bufferedWriter().use { out ->
////        out.write(iterationResults.joinToString { it.toString() })
////      }
////      execute(
////        configFile,
////        EndRun,
////        listOf(
////          benchmarkProgressPath,
////          runConfigPath,
////          iterationsResultsFile.absolutePath
////        )
////      )
////      runResults[runConfigPath] = iterationResults.joinToString()
////    }
////  }
////
////  private fun mergeResults(
////    configFile: File,
////    benchmarkProgressPath: String,
////    runResults: Map<String, String>,
////  ) {
////    val samplesFile = createTempFile("bench_results")
////    samplesFile.bufferedWriter().use { out ->
////      out.write(runResults.entries.joinToString("\n") { (k, v) -> "${k}: $v" })
////    }
////    execute(configFile, StoreResults, listOf(benchmarkProgressPath, samplesFile.absolutePath))
////  }
////
////  private fun execEndRun(configFile: File, args: List<String>) {
////    execute(configFile, EndRun, listOf())
////  }
////
////  private fun execStoreResults(configFile: File, args: List<String>) {
////    execute(configFile, StoreResults, listOf())
////  }
////
////  private fun execWarmup(configFile: File, args: List<String>) {
////    execute(configFile, Warmup, listOf())
////  }
////
////  private fun execIteration(configFile: File, args: List<String>) {
////    execute(configFile, Iteration, listOf())
////  }
////
////  private fun execBenchmark(configFile: File, args: List<String>) {
////    execute(configFile, Benchmark, listOf())
////  }
////
////  private fun listActiveBenchmarks(
////    configFile: File,
////    benchmarkProgressPath: File,
////    benchmarkDescriptionDir: File,
////  ): List<File> {
////    execute(
////      configFile,
////      ListRunning,
////      listOf(
////        //"--list",
////        benchmarkProgressPath.absolutePath,
////        benchmarkDescriptionDir.absolutePath,
////      )
////    )
////    return objects.fileTree().from(benchmarkDescriptionDir).files.sortedBy { it.absolutePath }
////  }
