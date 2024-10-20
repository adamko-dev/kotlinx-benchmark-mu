package kotlinx.benchmark.gradle.mu.config

sealed class ResultFormat(
  val format: String,
  val extension: String = format,
) {
  object Text : ResultFormat("text", "txt") {
    override fun toString(): String = "ResultFormat.Text"
  }

  object CSV : ResultFormat("csv") {
    override fun toString(): String = "ResultFormat.CSV"
  }
  /** Semicolon seperated values */
  object SCSV : ResultFormat("scsv") {
    override fun toString(): String = "ResultFormat.SCSV"
  }

  object JSON : ResultFormat("json") {
    override fun toString(): String = "ResultFormat.JSON"
  }

  class Custom(format: String, extension: String = format) : ResultFormat(format) {
    override fun toString(): String = "ResultFormat.Custom(format='$format', extension='$extension')"

    override fun equals(other: Any?): Boolean =
      this === other || (other is Custom && format == other.format && extension == other.extension)

    override fun hashCode(): Int = format.hashCode()
  }
//  /** LaTeX document */
//  object LaTeX : ResultFormat("latex")
}
