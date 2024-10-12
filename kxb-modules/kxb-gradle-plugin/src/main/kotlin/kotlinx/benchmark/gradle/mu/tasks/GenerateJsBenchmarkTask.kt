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
abstract class GenerateJsBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  @get:OutputDirectory
  abstract val generatedSources: DirectoryProperty

  @get:OutputDirectory
  abstract val generatedResources: DirectoryProperty

  @get:Classpath
  abstract val inputClasses: ConfigurableFileCollection

  @get:Classpath
  abstract val inputDependencies: ConfigurableFileCollection

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

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
