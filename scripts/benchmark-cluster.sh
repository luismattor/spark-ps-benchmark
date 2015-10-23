#!/bin/bash
#
# Run Parameter Server Benchmark on cluster
#
# title         benchmark-cluster.sh
# author        Luis Mateos (luismattor@gmail.com)
# date          31-07-2015
# usage         /benchmark-cluster.sh num-partitions num-iterations num-features [SparkUpdateBenchmark|PSUpdateBenchmark|PSBroadcastUpdateBenchmark] experiment-output.txt
# notes			Setup global variables according to experiment settings

set -e

spark_home=/usr/local/spark-ps
benchmark_jar=/home/luismattor/Projects/spark-ps-benchmark/target/spark-ps-benchmark-0.0.1-SNAPSHOT-jar-with-dependencies.jar
benchmark_class=$4
benchmark_output=$5

echo "Running benchmark $benchmark_class on cluster"
echo "$(date)";
$spark_home/bin/spark-submit \
		--master spark://mercado-9:7077 \
		--name $benchmark_class \
		--class "org.sparkps.$benchmark_class" \
		--executor-memory 14g \
		--driver-memory 14g \
		--conf "spark.eventLog.enabled=true" \
		--conf "spark.eventLog.dir=/datab/spark/" \
		--conf "spark.serializer=org.apache.spark.serializer.KryoSerializer" \
		$benchmark_jar $1 $2 $3 > $benchmark_output