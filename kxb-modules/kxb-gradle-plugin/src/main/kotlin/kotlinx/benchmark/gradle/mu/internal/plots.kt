package kotlinx.benchmark.gradle.mu.internal

import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

fun main() {
  val data = mapOf(
    "A" to 10.0,
    "B" to 20.5,
    "C" to 15.75,
    "D" to 30.0,
    "E" to 25.3,
  )

  val plot = barPlot(
    data = data,
  )
  println(plot)


  println("\n" + "-".repeat(10) + "\n")

  val bins =
    List(Random.nextInt(5..50)) { Random.nextInt(5..50) }.let {
      KBins(it, avg = Random.nextInt(5..50), peak = 50)
    }
//    KBins(listOf(1, 3, 5, 7, 9, 11, 13, 15), avg = 3, peak = 15)
//  val result = histogram(bins)
  println(histogram(bins))

  println("\n" + "-".repeat(10) + "\n")

  println(histogram(KBins(listOf(1, 3, 5, 7, 9, 11, 13, 15), avg = 3, peak = 15)))
//  result.forEach { println(it) }
}

internal class KBins(val bins: List<Int>, val avg: Int, val peak: Int)

internal fun histogram(
  bins: KBins,
  height: Int = 2, // must be 1 or 2
  colors: Boolean = true,
): String {
  val symbols = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")

  val scale: Double = (height * symbols.size - 1).toDouble() / bins.peak.toDouble()

//  val canvas: List<List<String>> =
//    List(bins.bins.size) { o ->
//      var s = (bins.bins[o] * scale).roundToInt()
//      List(height) { h ->
//        val leftover = s - symbols.size
//        //val clampedIndex = s.coerceIn(symbols.indices)
//        val symbol = symbols.getOrNull(s) ?: " "
//        s = leftover
//        symbol
//      }
//    }

  val canvas = Array(height) { Array(bins.bins.size) { " " } }
  for (o in bins.bins.indices) {
    var s = (bins.bins[o] * scale).roundToInt()

    for (h in 0 until height) {
      val leftover = s - symbols.size
      val clampedIndex = s.coerceIn(symbols.indices)
      canvas[h][o] = symbols[clampedIndex]
      if (s <= symbols.size) break
      s = leftover
    }
  }

  val cyan = if (colors) "\u001B[36m" else ""
  val yellow = if (colors) "\u001B[33m" else ""
  val magenta = if (colors) "\u001B[35m" else ""
  val reset = if (colors) "\u001B[0m" else ""

  val histogram = List(height) { h ->
    buildString {
      if (bins.avg > 0) {
        append(cyan)
        for (o in 0 until bins.avg) {
          append(canvas[h][o])
        }
        append(reset)
      }

      append(yellow)
      append(canvas[h][bins.avg])
      append(reset)

      if (bins.avg != bins.bins.size - 1) {
        append(magenta)
        for (o in bins.avg + 1 until bins.bins.size) {
          append(canvas[h][o])
        }
        append(reset)
      }
    }
  }

  return histogram.reversed().joinToString("\n")
}

internal fun barPlot(
  data: Map<String, Double>,
  legend: Int = data.keys.maxOf { it.length },
  width: Int = 14,
  colours: Boolean = true
): String {
  val min = data.minOf { it.value }
  val max = data.maxOf { it.value }
  val steps = width - 11
  val step = (max - min) / steps

  val gray = if (colours) "\u001B[90m" else ""
  val reset = if (colours) "\u001B[0m" else ""
  val yellow = if (colours) "\u001B[33m" else ""

  return buildString {
    append(" ".repeat(1 + legend))

    append("┌" + " ".repeat(width) + "┐")
    appendLine()

    for ((name, value) in data) {
      val offset = 1 + ((value - min) / step).roundToInt()
      append(name.padEnd(legend))
      append(" ┤")

      append(gray)
      append("■".repeat(offset))
      append(reset)

      append(" ")

      append(yellow)
      append("%.2f".format(Locale.ROOT, value))
      append(reset)

      appendLine()
    }

    append(" ".repeat(1 + legend))
    append("└" + " ".repeat(width) + "┘")
    appendLine()
  }
}
