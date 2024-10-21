package kotlinx.benchmark.gradle.mu.tasks.generate

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.GenerateWasmSourceWorker
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateWasmBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseGenerateBenchmarkTask() {

  @get:Input
  abstract val title: Property<String>

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
