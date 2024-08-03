package kotlinx.benchmark.gradle.mu.internal.utils

import java.util.*

internal fun String.uppercaseFirstChar(): String =
  replaceFirstChar { it.uppercase(Locale.ROOT) }
