package org.sparkps

// TODO Fix import. Current build uses DistML modified code.
//import org.sparkps.model.{UpdateModel, Sample}
import com.intel.distml.model.update.{UpdateModel, Sample}
import com.intel.distml.platform.{TrainingHelper, TrainingContext}
import org.apache.spark.{SparkContext, SparkConf}

object DistMLUpdateBenchmark {

	val ApplicationName = "DistML - Parameter Update Benchmark"

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

		var samples = new Array[Sample](NumPartitions)
		for(i <- 0 to samples.length-1) {
			samples(i) = new Sample(i);
		}
		val data = sc.parallelize(samples, NumPartitions)
		val model: UpdateModel = new UpdateModel(NumFeatures)
		val config = new TrainingContext().iteration(NumIterations);
		TrainingHelper.startTraining(sc, model, data, config)
		model.show()

		val real = 0.1 * NumIterations * NumPartitions * NumFeatures
		println("sum [real]         :\t%f".format(real))
		println("total [s]          :\t%f".format((System.nanoTime - start) / 1e9))
	}
}
