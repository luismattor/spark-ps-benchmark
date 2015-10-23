#!/bin/bash
#
# Run Parameter Server Benchmark locally
#
# title         benchmark-local.sh
# author        Luis Mateos (luismattor@gmail.com)
# date          31-07-2015
# usage         /benchmark-local.sh num-partitions num-iterations num-features [SparkUpdateBenchmark|PSUpdateBenchmark|PSBroadcastUpdateBenchmark] experiment-output.txt
# notes			Setup global variables according to experiment settings

set -e

spark_home=/usr/local/spark-ps
benchmark_jar=/home/luismattor/Projects/spark-ps-benchmark/target/spark-ps-benchmark-0.0.1-SNAPSHOT-jar-with-dependencies.jar
benchmark_class=$4
benchmark_output=$5

echo "Running benchmark $benchmark_class locally"
echo "$(date)";
$spark_home/bin/spark-submit \
		--master local[*] \
		--name $benchmark_class \
		--class "org.sparkps.$benchmark_class" \
		--executor-memory 2g \
		--driver-memory 2g \
		"$benchmark_jar" $1 $2 $3 > $benchmark_output

#	--conf spark.driver.extraJavaOptions=-javaagent:/home/luismattor/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.8.2.jar \
#	--conf spark.executor.extraJavaOptions=-javaagent:/home/luismattor/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.8.2.jar \
#	--driver-java-options -javaagent:/home/luismattor/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.8.2.jar \

