package kotlinx.benchmark.native

import kotlinx.benchmark.BenchmarkConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
internal data class BenchmarkRun(
  val benchmarkName: String,
  val config: BenchmarkConfiguration,
  val parameters: Map<String, String>,
) {

  companion object {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = kotlinx.serialization.json.Json {
      prettyPrint = true
      prettyPrintIndent = "  "
    }

    fun BenchmarkRun.encodeToJson(): String =
      json.encodeToString(serializer(), this)

    fun decodeFromJson(content: String): BenchmarkRun =
      json.decodeFromString(serializer(), content)
  }
}
