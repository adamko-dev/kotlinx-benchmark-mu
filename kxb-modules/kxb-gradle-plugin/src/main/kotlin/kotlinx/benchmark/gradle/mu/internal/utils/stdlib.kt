package kotlinx.benchmark.gradle.mu.internal.utils

import java.util.*

internal fun String.uppercaseFirstChar(): String =
  replaceFirstChar { it.uppercase(Locale.ROOT) }

internal fun String.lowercaseFirstChar(): String =
  replaceFirstChar { it.lowercase(Locale.ROOT) }

internal fun buildName(
  vararg name: String,
): String {
  require(name.isNotEmpty()) { "name $name must not be empty" }
  require(name.none { it.isBlank() }) { "name $name must not contain blank parts" }
  return name.joinToString("") { it.uppercaseFirstChar() }.lowercaseFirstChar()
}
