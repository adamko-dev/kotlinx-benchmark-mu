package kotlinx.benchmark.gradle.mu.tasks.generate

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.GenerateNativeSourceWorker
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateNativeBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseGenerateBenchmarkTask() {

  @get:Input
  abstract val title: Property<String>

  @get:Input
  abstract val targetName: Property<String>

  @TaskAction
  protected fun generate() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateNativeSourceWorker::class) {
      this.title.set(this@GenerateNativeBenchmarkTask.title)
      this.target.set(this@GenerateNativeBenchmarkTask.targetName)
      this.inputClasses.from(this@GenerateNativeBenchmarkTask.inputClasses)
      this.inputDependencies.from(this@GenerateNativeBenchmarkTask.inputDependencies)
      this.outputSourcesDir.set(this@GenerateNativeBenchmarkTask.generatedSources)
      this.outputResourcesDir.set(this@GenerateNativeBenchmarkTask.generatedResources)
    }

    workQueue.await()
  }
}
