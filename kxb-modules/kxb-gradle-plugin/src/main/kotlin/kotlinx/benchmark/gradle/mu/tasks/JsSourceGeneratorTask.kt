package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.JsBenchmarksExecutor
import kotlinx.benchmark.gradle.mu.workers.GenerateJsSourceWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class JsSourceGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

//  @get:Nested
//  abstract val benchmarkTarget: Property<BenchmarkTarget.Kotlin.JS>

//  @get:Input
//  abstract val title: String

//  @get:Input
//  abstract val useBenchmarkJs: Boolean

  @get:Classpath
  abstract val inputClasses: ConfigurableFileCollection

  @get:Classpath
  abstract val inputDependencies: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val generatedResources: DirectoryProperty

  @get:OutputDirectory
  abstract val generatedSources: DirectoryProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Input
  abstract val benchmarksExecutor: Property<JsBenchmarksExecutor>

  @get:Input
  abstract val title: Property<String>

  @TaskAction
  fun generate() {
//    val benchmarkTarget = benchmarkTarget.get()

    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateJsSourceWorker::class) {
      this.title = this@JsSourceGeneratorTask.title
      this.benchmarksExecutor = this@JsSourceGeneratorTask.benchmarksExecutor
      this.inputClasses.from(this@JsSourceGeneratorTask.inputClasses)
      this.inputDependencies = this@JsSourceGeneratorTask.inputDependencies
      this.outputResourcesDir = this@JsSourceGeneratorTask.generatedResources
      this.outputSourcesDir = this@JsSourceGeneratorTask.generatedSources
    }

    workQueue.await()
  }
}
