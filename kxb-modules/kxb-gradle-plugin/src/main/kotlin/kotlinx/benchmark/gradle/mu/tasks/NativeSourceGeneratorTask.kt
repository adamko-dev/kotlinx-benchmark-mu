package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.config.BenchmarkTarget
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

  @get:Nested
  abstract val benchmarkTarget: Property<BenchmarkTarget.Kotlin.Native>

  @get:Input
  abstract val title: String

  @get:Input
  abstract val nativeTarget: Property<String>

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
    val benchmarkTarget = benchmarkTarget.get()

    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(NativeSourceGeneratorWorker::class) {
      title.set(benchmarkTarget.title)
      target.set(benchmarkTarget.targetName)
      inputClasses.from(this@NativeSourceGeneratorTask.inputClasses)
      inputDependencies.from(this@NativeSourceGeneratorTask.inputDependencies)
      outputSourcesDir.set(this@NativeSourceGeneratorTask.outputSourcesDir)
      outputResourcesDir.set(this@NativeSourceGeneratorTask.outputResourcesDir)
    }

    workQueue.await()
  }
}
