package kotlinx.benchmark.gradle.mu.tasks

import javax.inject.Inject
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.mu.workers.JmhBytecodeGeneratorWorker
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit

@CacheableTask
abstract class GenerateJvmBenchmarkTask
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
  abstract val inputCompileClasspath: ConfigurableFileCollection

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

//    @Optional
//    @Input
//    var executableProvider: Provider<String> = project.provider { null }

  @TaskAction
  fun generate() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
//            if (executableProvider.isPresent) {
//              forkOptions.executable = executableProvider.get()
//            }
    }

    workQueue.submit(JmhBytecodeGeneratorWorker::class) {
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
