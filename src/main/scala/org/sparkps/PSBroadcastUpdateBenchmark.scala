package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import org.apache.spark.ps.PSContext
import org.apache.spark.ps.local.LocalPSConfig

object PSBroadcastUpdateBenchmark {

    val ApplicationName = "PS on Spark- Parameter Update with broadcast Benchmark"

    def main(args: Array[String]) {

        val NumPartitions = args(0).toInt

        val NumIterations = args(1).toInt

        val NumFeatures = args(2).toInt

        val start = System.nanoTime 
        val conf = new SparkConf().setAppName(ApplicationName).set("spark.akka.frameSize", "2000")
        val sc = new SparkContext(conf)

        println("------------------------------")
        println("Application ID:\t%s".format(sc.applicationId))
        println("Experiment.\tnp:\t%d\tnf:\t%d\tni:\t%d".format(NumPartitions, NumFeatures, NumIterations))

        val data = sc.parallelize(1 to NumPartitions, NumPartitions)
        val psContext = new PSContext(sc, LocalPSConfig(1, NumFeatures, 1))
        val initialParams = Array.fill[Double](NumFeatures)(0.0)
        psContext.start()
        psContext.uploadParams(Array(initialParams))
        val timer0 = System.nanoTime()
        var i = 0;
        while (i < NumIterations) {
            var timer1  = System.nanoTime()
            val params = psContext.downloadParams()(0)
            println("\tps-get          [s]:\t%f".format((System.nanoTime() - timer1) / 1e9))
            timer1  = System.nanoTime()
            val bdparams = data.context.broadcast(params)
            println("\tps-broadcast    [s]:\t%f".format((System.nanoTime() - timer1) / 1e9))
            timer1  = System.nanoTime()
            psContext.runPSJob(data)((index, arr, client) => {
                var timer2 = System.nanoTime
                val delta = Array.fill[Double](NumFeatures)(0.1)
                println("\tps-delta        [s]:\t%f".format((System.nanoTime() - timer2) / 1e9))
                timer2 = System.nanoTime
                client.update(0, delta)
                client.clock()
                println("\tps-update-clock [s]:\t%f".format((System.nanoTime() - timer2) / 1e9))
                Iterator()
                }).count()
            println("\tps-job          [s]:\t%f".format((System.nanoTime() - timer1) / 1e9))
            i += 1
        }
        println("update-average [s]:\t%.6f".format((System.nanoTime() - timer0) / 1e9 / NumIterations))
        val sum1 = psContext.downloadParams()(0).sum
        val sum2 = 0.1 * NumIterations * NumPartitions * NumFeatures
        psContext.stop()
        println("output  [computed]:\t%f".format(sum1))
        println("output      [real]:\t%f".format(sum2))
        println("total          [s]:\t%f".format((System.nanoTime - start) / 1e9))
        assert(Math.abs(sum1 - sum2) < 0.001, "Parameter Server Error")

    }

}