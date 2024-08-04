package kotlinx.benchmark

//import kotlinx.serialization.Serializable
//
//@Serializable
//sealed class ResultFormat {
//  abstract val format: String
//  open val extension: String get() = format
//
//  @Serializable
//  object Text : ResultFormat() {
//    override val format: String = "text"
//    override val extension: String = "txt"
//    override fun toString(): String = "ResultFormat.Text"
//  }
//
//  @Serializable
//  object CSV : ResultFormat() {
//    override val format: String = "csv"
//    override fun toString(): String = "ResultFormat.CSV"
//  }
//
//  /** Semicolon seperated values */
//  @Serializable
//  object SCSV : ResultFormat() {
//    override val format: String = "scsv"
//    override fun toString(): String = "ResultFormat.SCSV"
//  }
//
//  @Serializable
//  object JSON : ResultFormat() {
//    override val format: String = "json"
//    override fun toString(): String = "ResultFormat.JSON"
//  }
//
//  @Serializable
//  class Custom(
//    override val format: String,
//    override val extension: String = format,
//  ) : ResultFormat() {
//
//    override fun toString(): String = "ResultFormat.Custom(format='$format', extension='$extension')"
//
//    override fun equals(other: Any?): Boolean =
//      this === other || (other is Custom && format == other.format && extension == other.extension)
//
//    override fun hashCode(): Int = format.hashCode()
//  }
////  /** LaTeX document */
////  object LaTeX : ResultFormat("latex")
//}
