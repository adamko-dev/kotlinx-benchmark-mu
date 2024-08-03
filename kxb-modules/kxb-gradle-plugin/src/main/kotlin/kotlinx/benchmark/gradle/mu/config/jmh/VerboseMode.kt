package kotlinx.benchmark.gradle.mu.config.jmh

enum class VerboseMode(private val level: Int) {
  /** Be completely silent. */
  Silent(0),

  /** Output normally. */
  Normal(1),

  /** Output extra info. */
  Extra(2),
  ;

  internal infix fun equalsOrHigherThan(other: VerboseMode): Boolean = level >= other.level
}
