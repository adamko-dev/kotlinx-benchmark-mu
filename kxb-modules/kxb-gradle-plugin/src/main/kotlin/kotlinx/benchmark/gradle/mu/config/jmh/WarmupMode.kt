package kotlinx.benchmark.gradle.mu.config.jmh

/** Warmup Mode */
enum class WarmupMode(
  private val isBulk: Boolean,
  private val isIndividual: Boolean
) {
  /** Do the individual warmup for every benchmark. */
  Individual(false, true),

  /** Do the bulk warmup before any benchmark starts. */
  Bulk(true, false),

  /** Do the bulk warmup before any benchmark starts, and then also do individual warmups for every benchmark. */
  BulkIndividual(true, true),
}
