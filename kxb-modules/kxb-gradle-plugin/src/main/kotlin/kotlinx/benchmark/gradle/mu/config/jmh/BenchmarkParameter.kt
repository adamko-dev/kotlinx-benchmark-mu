package kotlinx.benchmark.gradle.mu.config.jmh

import javax.inject.Inject
import org.gradle.api.Named

abstract class BenchmarkParameter @Inject constructor(
  private val name: String
) : Named {
  override fun getName(): String = name
}
