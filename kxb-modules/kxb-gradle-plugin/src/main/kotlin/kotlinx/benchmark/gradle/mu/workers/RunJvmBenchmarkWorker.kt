package kotlinx.benchmark.gradle.mu.workers

import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.jvm.runJvmBenchmark
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters


@KotlinxBenchmarkPluginInternalApi
internal abstract class RunJvmBenchmarkWorker : WorkAction<RunJvmBenchmarkWorker.Parameters> {

  @KotlinxBenchmarkPluginInternalApi
  interface Parameters : WorkParameters {
    val config: Property<String>
    val classpath: ConfigurableFileCollection
  }

  @OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
  override fun execute() {
    val configString = parameters.config.get()
    println(
      "Running RunJvmBenchmarkWorker with config: " +
          "---" +
          configString.prependIndent("\t") +
          "---"
    )

//    org.openjdk.jmh.runner.ForkedMain::class.java

//    val classpath = StringBuilder()
//    for (url in RunnerConfiguration::class.java.classLoader.getURLs()) {
//      classpath.append(url.path).append(File.pathSeparator)
//    }
    System.setProperty("java.class.path", parameters.classpath.asPath)
    println("java.class.path ${System.getProperty("java.class.path")}")

    val config = RunnerConfiguration(configString)
    runJvmBenchmark(config)
  }
}
