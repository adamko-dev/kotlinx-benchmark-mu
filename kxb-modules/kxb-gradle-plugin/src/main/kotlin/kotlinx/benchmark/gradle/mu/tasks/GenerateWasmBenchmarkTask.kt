package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.GenerateWasmSourceWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateWasmBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  @get:OutputDirectory
  abstract val generatedSources: DirectoryProperty

  @get:OutputDirectory
  abstract val generatedResources: DirectoryProperty

  @get:Input
  abstract val title: String

  @get:Classpath
  abstract val inputClasses: ConfigurableFileCollection

  @get:Classpath
  abstract val inputDependencies: ConfigurableFileCollection

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @TaskAction
  protected fun generate() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateWasmSourceWorker::class) {
      title.set(this@GenerateWasmBenchmarkTask.title)
      inputClasses.from(this@GenerateWasmBenchmarkTask.inputClasses)
      inputDependencies.from(this@GenerateWasmBenchmarkTask.inputDependencies)
      outputSourcesDir.set(this@GenerateWasmBenchmarkTask.generatedSources)
      outputResourcesDir.set(this@GenerateWasmBenchmarkTask.generatedResources)
    }
  }
}
