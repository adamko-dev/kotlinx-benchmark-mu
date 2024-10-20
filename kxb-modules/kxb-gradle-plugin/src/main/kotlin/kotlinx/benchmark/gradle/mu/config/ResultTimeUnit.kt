package kotlinx.benchmark.gradle.mu.config

/**
 * Time unit used in Benchmark reports.
 */
sealed class ResultTimeUnit {
  abstract val unit: String

  object Minutes : ResultTimeUnit() {
    override val unit: String = "MINUTES"
    override fun toString(): String = "ResultTimeUnit.Minutes"
  }

  object Seconds : ResultTimeUnit() {
    override val unit: String = "SECONDS"
    override fun toString(): String = "ResultTimeUnit.Seconds"
  }

  object Milliseconds : ResultTimeUnit() {
    override val unit: String = "MILLISECONDS"
    override fun toString(): String = "ResultTimeUnit.Milliseconds"
  }

  object Microseconds : ResultTimeUnit() {
    override val unit: String = "MICROSECONDS"
    override fun toString(): String = "ResultTimeUnit.Microsecond"
  }

  object Nanoseconds : ResultTimeUnit() {
    override val unit: String = "NANOSECONDS"
    override fun toString(): String = "ResultTimeUnit.Nanoseconds"
  }

  class Custom(override val unit: String) : ResultTimeUnit() {
    override fun toString(): String = "ResultTimeUnit.Custom($unit)"

    override fun equals(other: Any?): Boolean =
      this === other || unit == (other as? Custom)?.unit

    override fun hashCode(): Int = unit.hashCode()
  }
}
