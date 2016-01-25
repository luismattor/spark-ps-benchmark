package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import org.apache.spark.ps.PSContext
import org.apache.spark.ps.local.LocalPSConfig

object PSUpdateBenchmark {

    val ApplicationName = "PS on Spark- Parameter Update Benchmark"

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
        val time = System.nanoTime()
        psContext.runPSJob(data)((index, arr, client) => {
            println("******************************")
            for (i <- 0 until NumIterations) {
                val w = client.get(0)
                val r = new java.util.Random(301214L) 
                val delta = Array.fill[Double](NumFeatures)(r.nextFloat)
                client.update(0, delta)
                client.clock()
            }
            println("******************************")
            Iterator()
            }).count()
        println("average[s]    :\t%f".format((System.nanoTime() - time) / 1e9 / NumIterations))
        val sum1 = psContext.downloadParams()(0).sum
        val r = new java.util.Random(301214L)
        val sum2 = NumIterations * NumPartitions * Array.fill[Double](NumFeatures)(r.nextFloat).sum
        psContext.stop()
        println("sum [computed]:\t%f".format(sum1))
        println("sum [real]    :\t%f".format(sum2))
        println("total [s]     :\t%f".format((System.nanoTime - start) / 1e9))
        assert(Math.abs(sum1 - sum2) < 0.001, "Parameter Server Error")

    }

}
