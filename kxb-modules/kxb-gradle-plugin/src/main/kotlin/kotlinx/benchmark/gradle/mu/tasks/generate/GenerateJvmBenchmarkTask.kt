package kotlinx.benchmark.gradle.mu.tasks.generate

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.GenerateJvmJmhSourceWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateJvmBenchmarkTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor() : BaseGenerateBenchmarkTask() {

  @get:Classpath
  abstract val inputCompileClasspath: ConfigurableFileCollection

  @TaskAction
  protected fun generate() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(GenerateJvmJmhSourceWorker::class) {
      this.inputClasses.from(this@GenerateJvmBenchmarkTask.inputClasses)
      this.inputClasspath.from(this@GenerateJvmBenchmarkTask.inputCompileClasspath)
      this.outputSourceDir.set(this@GenerateJvmBenchmarkTask.generatedSources)
      this.outputResourceDir.set(this@GenerateJvmBenchmarkTask.generatedResources)
    }

    workQueue.await()

    // TODO combine the other tasks into this task
    //      - run javac
    //      - run java -jar
    //        (might not need to JAR the classes though... can just pass the classes directly to the exec?)
    //
  }
}
