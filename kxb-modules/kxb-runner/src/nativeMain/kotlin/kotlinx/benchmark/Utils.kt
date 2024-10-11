package kotlinx.benchmark

import kotlinx.benchmark.native.BenchmarkRun
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.*

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
  val longPart = toLong()
  val fractional = this - longPart
  val thousands =
    if (useGrouping) longPart.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
    else longPart.toString()
  if (precision == 0)
    return thousands

  return memScoped {
    val bytes = allocArray<ByteVar>(100)
    sprintf(bytes, "%.${precision}f", fractional)
    val fractionText = bytes.toKString()
    thousands + fractionText.removePrefix("0")
  }
}

internal actual fun String.writeFile(text: String) {
  //println("Writing to file $this")
  val file = fopen(this, "w")
  try {
    val result = fputs(text, file)
    if (result == EOF) {
      error("File write error $result")
    }
  } catch (ex: Exception) {
    println("Failed to write file $this - ${ex.message}")
    throw ex
  } finally {
    fclose(file)
  }
}

internal actual fun String.readFile(): String {
  //println("Reading file $this")
  val file = fopen(this@readFile, "rb")
  try {
    memScoped {
      val bufferLength = 64 * 1024
      val buffer = allocArray<ByteVar>(bufferLength)
      return buildString {
        while (true) {
          val line = fgets(buffer, bufferLength, file)?.toKString() // newline symbol is included
          if (line.isNullOrEmpty()) break
          append(line)
        }
      }
    }
  } catch (ex: Exception) {
    println("Failed to read file $this - ${ex.message}")
    throw ex
  } finally {
    fclose(file)
  }
}

internal fun String.parseBenchmarkConfig(): BenchmarkRun {
//    fun String.getElement(name: String) =
//        if (startsWith(name)) {
//            substringAfter("$name: ")
//        } else throw NoSuchElementException("Parameter `$name` is required.")

  val content = readFile()
  return BenchmarkRun.decodeFromJson(content)
//    val lines = content.lines().filter { it.isNotEmpty() }
//    require(lines.size == 3) { "Wrong format of detailed benchmark configuration file. " }
//    val name = lines[0].getElement("benchmark")
//    val configurationJson = lines[1].getElement("configuration")
//    val configuration = BenchmarkConfiguration.decodeFromJson(configurationJson)
//    val parameters = lines[2].getElement("parameters").parseMap()
//    return  BenchmarkRun(name, configuration, parameters)
}

internal actual inline fun measureNanoseconds(block: () -> Unit): Long = TODO("Not implemented for this platform")
