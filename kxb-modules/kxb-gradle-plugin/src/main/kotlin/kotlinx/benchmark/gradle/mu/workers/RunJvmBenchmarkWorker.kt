package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.jvm.runJvmBenchmark
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@KotlinxBenchmarkPluginInternalApi
internal abstract class RunJvmBenchmarkWorker : WorkAction<RunJvmBenchmarkWorker.Parameters> {

  @KotlinxBenchmarkPluginInternalApi
  interface Parameters : WorkParameters {
    val config: Property<String>
    val classpath: ConfigurableFileCollection
    val enableDemoMode: Property<Boolean>
  }

  @OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
  override fun execute() {
    val isDemoMode = parameters.enableDemoMode.getOrElse(false)

    val config = parameters.config.get()

    logger.info(
      "Running RunJvmBenchmarkWorker with config: " +
          "---\n$config\n---".prependIndent("\t")
    )

//    org.openjdk.jmh.runner.ForkedMain::class.java

//    val classpath = StringBuilder()
//    for (url in RunnerConfiguration::class.java.classLoader.getURLs()) {
//      classpath.append(url.path).append(File.pathSeparator)
//    }
    if (isDemoMode) {
      System.setProperty("jmh.ignoreLock", "true")

//      overrideBenchmarkList()
    }

    System.setProperty("java.class.path", parameters.classpath.asPath)
    logger.info("java.class.path ${System.getProperty("java.class.path")}")

    runJvmBenchmark(
      RunnerConfiguration(config),
      demoMode = isDemoMode,
    )
  }

//  private fun overrideBenchmarkList() {
//      val list = BenchmarkList.readBenchmarkList(javaClass.getResourceAsStream(BENCHMARK_LIST))
//      val updated = list.map {
//        BenchmarkListEntry(
//          it.userClassQName,
//          "kotlinx.benchmark.jvm.FakeBenchmark",//generatedClassQName,//
//          "fakeJmhBenchmark",//method,//
//          it.mode,
//          it.threads,
//          it.threadGroups,
//          it.threadGroupLabels,
//          it.warmupIterations,
//          it.warmupTime,
//          it.warmupBatchSize,
//          it.measurementIterations,
//          it.measurementTime,
//          it.measurementBatchSize,
//          it.forks,
//          it.warmupForks,
//          it.jvm,
//          it.jvmArgs,
//          it.jvmArgsPrepend,
//          it.jvmArgsAppend,
//          it.params,
//          it.timeUnit,
//          it.operationsPerInvocation,
//          it.timeout,
//        )
//      }
//      val bmlUrl = javaClass.getResource(BENCHMARK_LIST)
//        ?: error("Missing BENCHMARK_LIST $BENCHMARK_LIST")
//      File(bmlUrl.toURI())
////      bmlUrl.toURI().toFile()
//        .outputStream().use { out ->
//        BenchmarkList.writeBenchmarkList(out, updated)
//      }
//  }

  @KotlinxBenchmarkPluginInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(RunJvmBenchmarkWorker::class.java)
  }
}
