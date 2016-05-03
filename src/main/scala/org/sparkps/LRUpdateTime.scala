package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import org.sparkps.util.MultiArrayAggregator

object LRUpdateBenchmark {

  val ApplicationName = "Spark - MultiArray Update Benchmark"

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
    val r1 = new java.util.Random(301214L);
    var weights = Array.fill[Double](NumFeatures)(r1.nextFloat)
    val time = System.nanoTime()
    for (i <- 0 until NumIterations) {
      val r2 = new java.util.Random(301215L);
      val array2 = Array.fill[Double](NumFeatures)(r2.nextFloat)
      val r3 = new java.util.Random(301216L);
      val array3 = Array.fill[Double](NumFeatures)(r3.nextFloat)
      val seqOp = (c: MultiArrayAggregator, instance: Int) => c.add(instance)
      val combOp = (c1: MultiArrayAggregator, c2: MultiArrayAggregator) => c1.merge(c2)

      val res = data.treeAggregate(new MultiArrayAggregator(weights, array2, array3))(seqOp, combOp)
      weights = weights.zip(res.accum).map(t => t._1 + t._2)
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
