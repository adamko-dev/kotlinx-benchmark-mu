package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
import kotlinx.benchmark.gradle.mu.workers.GenerateJsSourceWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class JsSourceGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  @get:Nested
  abstract val benchmarkTarget: Property<BenchmarkTarget.Kotlin.JS>

//  @get:Input
//  abstract val title: String

//  @get:Input
//  abstract val useBenchmarkJs: Boolean

  @get:Classpath
  abstract val inputClasses: ConfigurableFileTree

  @get:Classpath
  abstract val inputDependencies: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputResourcesDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputSourcesDir: DirectoryProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @TaskAction
  fun generate() {
    val benchmarkTarget = benchmarkTarget.get()

    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateJsSourceWorker::class) {
      this.title.set(benchmarkTarget.title)
      this.benchmarksExecutor.set(benchmarkTarget.benchmarksExecutor)
      this.inputClasses.from(this@JsSourceGeneratorTask.inputClasses)
      this.inputDependencies.from(this@JsSourceGeneratorTask.inputDependencies)
      this.outputResourcesDir.set(this@JsSourceGeneratorTask.outputResourcesDir)
      this.outputSourcesDir.set(this@JsSourceGeneratorTask.outputSourcesDir)
    }

    workQueue.await()
  }
}
