package kotlinx.benchmark.integration

import kotlin.test.Test

class KotlinNativeTest : GradleTest() {
    @Test
    fun debugBenchmarkTest() {
        project("kotlin-native", true).let { runner ->
            val target = "native"
            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }

            runner.run(":${target}BenchmarkGenerate")
            runner.run(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
            runner.run(":${capitalizedTarget}Benchmark")
        }
    }
}
