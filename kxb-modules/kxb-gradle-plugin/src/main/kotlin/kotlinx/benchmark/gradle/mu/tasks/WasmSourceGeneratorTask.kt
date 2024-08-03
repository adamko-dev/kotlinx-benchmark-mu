package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.GenerateWasmSourceWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class WasmSourceGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  @get:Input
  abstract val title: String

  @get:Classpath
  abstract val inputClasses: ConfigurableFileCollection

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
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateWasmSourceWorker::class) {
      title.set(this@WasmSourceGeneratorTask.title)
      inputClasses.from(this@WasmSourceGeneratorTask.inputClasses)
      inputDependencies.from(this@WasmSourceGeneratorTask.inputDependencies)
      outputSourcesDir.set(this@WasmSourceGeneratorTask.outputSourcesDir)
      outputResourcesDir.set(this@WasmSourceGeneratorTask.outputResourcesDir)
    }
  }
}
