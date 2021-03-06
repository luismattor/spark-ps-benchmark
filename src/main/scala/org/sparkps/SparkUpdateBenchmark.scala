package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object SparkUpdateBenchmark {

    val ApplicationName = "Spark - Parameter Update Benchmark"

    def main(args: Array[String]) {

        val NumPartitions = args(0).toInt

        val NumIterations = args(1).toInt

        val NumFeatures = args(2).toInt

        val start = System.nanoTime 
        val conf = new SparkConf().setAppName(ApplicationName)
        val sc = new SparkContext(conf)

        println("------------------------------")
        println("Application ID:\t%s".format(sc.applicationId))
        println("Experiment.\tnp:\t%d\tnf:\t%d\tni:\t%d".format(NumPartitions, NumFeatures, NumIterations))

        val data = sc.parallelize(1 to NumPartitions, NumPartitions)
        var weights = Array.fill[Double](NumFeatures)(0.0)
        val time = System.nanoTime()
        for (i <- 0 until NumIterations) {
            val bcWeights = data.context.broadcast(weights)      
            val sum = data.treeAggregate[Array[Double]](Array.fill[Double](NumFeatures)(0.0)) ( 
                seqOp = (c, v) => { val r = new java.util.Random(301214L); c.zip(Array.fill(NumFeatures)(r.nextFloat)).map(t => t._1 + t._2) }, 
                combOp = (c1, c2) => c1.zip(c2).map(t => t._1 + t._2) 
            )
            weights = weights.zip(sum).map(t => t._1 + t._2)
        }

        println("average[s]    :\t%f".format((System.nanoTime() - time) / 1e9 / NumIterations))
        val sum1 = weights.sum
        val r = new java.util.Random(301214L)
        var sum2 = NumIterations * NumPartitions *  Array.fill[Double](NumFeatures)(r.nextFloat).sum
        println("sum [computed]:\t%f".format(sum1))
        println("sum [real]    :\t%f".format(sum2))
        println("total [s]     :\t%f".format((System.nanoTime - start) / 1e9))
        assert(Math.abs(sum1 - sum2) < 0.001, "Parameter Server Error")

    }

}
