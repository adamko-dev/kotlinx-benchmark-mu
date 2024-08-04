package kotlinx.benchmark.jvm

import org.openjdk.jmh.infra.ThreadParams
import org.openjdk.jmh.results.BenchmarkTaskResult
import org.openjdk.jmh.results.RawResults
import org.openjdk.jmh.results.ResultRole
import org.openjdk.jmh.results.ThroughputResult
import org.openjdk.jmh.runner.InfraControl


@Suppress("FunctionName", "unused", "UNUSED_PARAMETER")
open class DryRunBenchmark {

  fun dryRun_Throughput(
    control: InfraControl,
    threadParams: ThreadParams,
  ): BenchmarkTaskResult {
    val res = RawResults()

    control.preSetup()
    control.announceWarmupReady()

    while (control.warmupShouldWait) {
      res.allOps++
    }

    control.notifyControl.startMeasurement = true
    throughputStub(control, res)
    control.notifyControl.stopMeasurement = true
    control.announceWarmdownReady()

    while (control.warmdownShouldWait) {
      res.allOps++
    }

    control.preTearDown()

    res.allOps += res.measuredOps
    val batchSize = control.iterationParams.batchSize.toLong()
    val opsPerInv = control.benchmarkParams.opsPerInvocation.toLong()
    res.allOps *= opsPerInv
    res.allOps /= batchSize
    res.measuredOps *= opsPerInv
    res.measuredOps /= batchSize

    val results = BenchmarkTaskResult(res.allOps, res.measuredOps)
    results.add(
      ThroughputResult(
        ResultRole.PRIMARY,
        control.benchmarkParams.benchmark,
//      "cosBenchmark",
        res.measuredOps.toDouble(),
        res.time,
        control.benchmarkParams.timeUnit
      )
    )
    return results
  }


  private fun throughputStub(
    control: InfraControl,
    result: RawResults,
  ) {
    var operations: Long = 0
    val realTime: Long = 0
    result.startTime = System.nanoTime()
    do {
      operations++
    } while (!control.isDone)
    result.stopTime = System.nanoTime()
    result.realTime = realTime
    result.measuredOps = operations
  }

}
