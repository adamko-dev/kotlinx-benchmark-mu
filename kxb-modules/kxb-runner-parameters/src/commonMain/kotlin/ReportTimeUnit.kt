package kotlinx.benchmark

import kotlinx.serialization.Serializable

/**
 * Time unit used in Benchmark reports.
 */
@Serializable
sealed class ReportTimeUnit {
  abstract val unit: String

  @Serializable
  object Minutes : ReportTimeUnit() {
    override val unit: String = "MINUTES"
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Minutes"
  }

  @Serializable
  object Seconds : ReportTimeUnit() {
    override val unit: String = "SECONDS"
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Seconds"
  }

  @Serializable
  object Milliseconds : ReportTimeUnit() {
    override val unit: String = "MILLISECONDS"
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Milliseconds"
  }

  @Serializable
  object Microsecond : ReportTimeUnit() {
    override val unit: String = "MICROSECONDS"
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Microsecond"
  }

  @Serializable
  object Nanoseconds : ReportTimeUnit() {
    override val unit: String = "NANOSECONDS"
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Nanoseconds"
  }

  @Serializable
  class Custom(override val unit: String) : ReportTimeUnit() {
    override fun toString(): String = "kotlinx.benchmark.ReportTimeUnit.Custom($unit)"

    override fun equals(other: Any?): Boolean =
      this === other || (other is Custom && unit == other.unit)

    override fun hashCode(): Int = unit.hashCode()
  }
}
