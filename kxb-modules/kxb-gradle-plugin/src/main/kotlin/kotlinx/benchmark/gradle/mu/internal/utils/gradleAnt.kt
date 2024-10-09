package kotlinx.benchmark.gradle.mu.internal.utils

import java.io.File
import org.gradle.api.AntBuilder
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.intellij.lang.annotations.Language

internal fun AntBuilder.get(
  @Language("http-url-reference") src: String,
  dest: File,
) {
  withGroovyBuilder {
    "get"(
      "src" to src,
      "dest" to dest,
    )
  }
}
