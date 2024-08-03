package kotlinx.benchmark.gradle.mu.internal

import kotlinx.benchmark.gradle.mu.tasks.KxbBaseTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

internal class KxbTasks(
  private val project: Project,
  private val kxbDependencies: KxbDependencies,
) {
  val benchmarks by project.tasks.registering(KxbBaseTask::class) {
    group = BenchmarksTaskGroup
    description = "Lifecycle task for running all benchmarks."
  }

  val assembleBenchmarks by project.tasks.registering {
    group = BenchmarksTaskGroup
    description = "Generate and build all benchmarks in a project."
  }


  companion object {
    @Suppress("ConstPropertyName")
    const val BenchmarksTaskGroup = "benchmark"
  }

}
