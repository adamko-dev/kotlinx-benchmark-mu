package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.NativeSourceGeneratorWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class NativeSourceGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : KxbBaseTask() {

  @get:Input
  abstract val title: Property<String>

  @get:Input
  abstract val targetName: Property<String>

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

  @TaskAction
  fun generate() {
//    val benchmarkTarget = benchmarkTarget.get()

    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(NativeSourceGeneratorWorker::class) {
      this.title.set(this@NativeSourceGeneratorTask.title)
      this.target.set(this@NativeSourceGeneratorTask.targetName)
      this.inputClasses.from(this@NativeSourceGeneratorTask.inputClasses)
      this.inputDependencies.from(this@NativeSourceGeneratorTask.inputDependencies)
      this.outputSourcesDir.set(this@NativeSourceGeneratorTask.generatedSources)
      this.outputResourcesDir.set(this@NativeSourceGeneratorTask.generatedResources)
    }

    workQueue.await()
  }
}
