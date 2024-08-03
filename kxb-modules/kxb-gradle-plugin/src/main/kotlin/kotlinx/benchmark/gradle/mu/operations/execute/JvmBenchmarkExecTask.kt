package kotlinx.benchmark.gradle.mu.operations.execute

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class JvmBenchmarkExecTask : DefaultTask() {

  @get:Inject
  protected val exec: ExecOperations get() = error("injected")


  @TaskAction
  fun x() {
  }
}
