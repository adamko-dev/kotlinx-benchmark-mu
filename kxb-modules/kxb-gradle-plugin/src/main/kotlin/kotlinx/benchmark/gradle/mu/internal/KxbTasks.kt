package kotlinx.benchmark.gradle.mu.internal

import kotlinx.benchmark.gradle.mu.tasks.BaseBenchmarkTask
import kotlinx.benchmark.gradle.mu.tasks.tools.D8SetupTask
import kotlinx.benchmark.gradle.mu.tasks.tools.NodeJsSetupTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

internal class KxbTasks(
  private val project: Project,
  private val kxbDependencies: KxbDependencies,
) {
  val benchmarks: TaskProvider<BaseBenchmarkTask> by project.tasks.registering(BaseBenchmarkTask::class) {
    group = BenchmarksTaskGroup
    description = "Lifecycle task for running all benchmarks."
  }

  val assembleBenchmarks: TaskProvider<Task> by project.tasks.registering {
    group = BenchmarksTaskGroup
    description = "Generate and build all benchmarks in a project."
  }

  val setupD8BenchmarkRunner: TaskProvider<D8SetupTask> by project.tasks.registering(D8SetupTask::class) {

  }

  val setupNodeJsBenchmarkRunner: TaskProvider<NodeJsSetupTask> by project.tasks.registering(NodeJsSetupTask::class) {

  }

  companion object {
    @Suppress("ConstPropertyName")
    const val BenchmarksTaskGroup = "benchmark"
  }
}
