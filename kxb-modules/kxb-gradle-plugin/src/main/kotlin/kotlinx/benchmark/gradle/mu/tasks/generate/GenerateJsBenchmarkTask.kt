package kotlinx.benchmark.gradle.mu.tasks.generate

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.JsBenchmarksExecutor
import kotlinx.benchmark.gradle.mu.workers.GenerateJsSourceWorker
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateJsBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseGenerateBenchmarkTask() {

  @get:Input
  abstract val benchmarksExecutor: Property<JsBenchmarksExecutor>

  @get:Input
  abstract val title: Property<String>

  @TaskAction
  protected fun generate() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateJsSourceWorker::class) {
      this.title = this@GenerateJsBenchmarkTask.title
      this.benchmarksExecutor = this@GenerateJsBenchmarkTask.benchmarksExecutor
      this.inputClasses.from(this@GenerateJsBenchmarkTask.inputClasses)
      this.inputDependencies = this@GenerateJsBenchmarkTask.inputDependencies
      this.outputResourcesDir = this@GenerateJsBenchmarkTask.generatedResources
      this.outputSourcesDir = this@GenerateJsBenchmarkTask.generatedSources
    }

    workQueue.await()
  }
}
