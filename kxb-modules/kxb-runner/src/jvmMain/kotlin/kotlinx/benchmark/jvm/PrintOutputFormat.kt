package kotlinx.benchmark.jvm

import java.io.PrintStream
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import org.openjdk.jmh.runner.format.OutputFormat
import org.openjdk.jmh.runner.options.VerboseMode

@KotlinxBenchmarkRuntimeInternalApi
abstract class PrintOutputFormat(private val out: PrintStream, private val verbose: VerboseMode = VerboseMode.NORMAL) :
  OutputFormat {

  override fun print(s: String) {
    if (verbose != VerboseMode.SILENT)
      out.print(s)
  }

  override fun verbosePrintln(s: String) {
    if (verbose == VerboseMode.EXTRA)
      out.println(s)
  }

  override fun write(b: Int) {
    if (verbose != VerboseMode.SILENT)
      out.print(b)
  }

  override fun write(b: ByteArray) {
    if (verbose != VerboseMode.SILENT)
      out.print(b)
  }

  override fun println(s: String) {
    if (verbose != VerboseMode.SILENT)
      out.println(s)
  }

  override fun flush() = out.flush()
  override fun close() = flush()
}
