package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import org.parameterserver.client.PSClient
import org.parameterserver.protocol.{DoubleArray, DataType}

object PSZenUpdateBenchmark {
    
    val ApplicationName = "PS Zen - Parameter Update Benchmark"

    val PSMasterAddress = "localhost:10010"

    val PSNamespace = "ps"

    val PSParamName = "w"

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
        val weights = Array.fill[Double](NumFeatures)(0.0)
        val indices = (0 until NumFeatures).toArray
        val time = System.nanoTime()

        val psClient = new PSClient(PSMasterAddress)
        psClient.setContext(PSNamespace)
        psClient.createVector(PSParamName, true, NumFeatures, DataType.Double, true)
        psClient.updateVector(PSParamName, new DoubleArray(weights))

        for (i <- 0 until NumIterations) {
            data.mapPartitions { iter =>
                val psClient = new PSClient(PSMasterAddress)
                psClient.setContext(PSNamespace)
                val w = psClient.getVector(PSParamName).getValues.asInstanceOf[DoubleArray].getValues
                val delta = Array.fill[Double](NumFeatures)(0.1)
                psClient.add2Vector(PSParamName, indices, new DoubleArray(delta))
                psClient.close()
                Iterator()
            }.count()
        }

        println("average[s]    :\t%f".format((System.nanoTime() - time) / 1e9 / NumIterations))
        val sum1 = psClient.getVector(PSParamName).getValues.asInstanceOf[DoubleArray].getValues.sum
        val sum2 = 0.1 * NumIterations * NumPartitions * NumFeatures
        psClient.removeVector(PSParamName)
        psClient.close()

        println("sum [computed]:\t%f".format(sum1))
        println("sum [real]    :\t%f".format(sum2))
        println("total [s]     :\t%f".format((System.nanoTime - start) / 1e9))
        assert(Math.abs(sum1 - sum2) < 0.001, "Parameter Server Error")
    }
}
