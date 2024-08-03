package kotlinx.benchmark.gradle.mu.config.jmh

enum class ResultFormat {
  Text,
  CSV,
  /** Semicolon seperated values */
  SCSV,
  JSON,
  /** LaTeX document */
  LaTeX,
}
