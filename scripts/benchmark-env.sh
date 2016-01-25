#!/bin/bash

scriptdir="$(dirname $(readlink -f $0))"

# Benchmark jar
benchmark_jar=$scriptdir/../target/spark-ps-benchmark-0.0.1-SNAPSHOT-jar-with-dependencies.jar
# Spark installation dir
spark_home=/usr/local/spark-ps
# Remote worker log dir 
workerlog_dir_remote=/usr/local/spark-ps/work
# Experiment outputs dir
experiments_dir=$scriptdir/../experiments/spark
# History log dir
eventlog_dir=$experiments_dir/history
# Workers log dir
workerlog_dir=$experiments_dir/work
# Worker nodes for fetching logs in cluster mode
nodes=("node-0" "node-1" "node-2" "node-4" "node-5")
#nodes=($(eval echo worker{0..15}))
# Remote use for connecting to workers
worker_user=hduser

# Script parameters
mode=$1
benchmark_class=$2
n_partitions=$3
n_iterations=$4
n_features=$5
benchmark_output=$6

# Default spark configuration
master="local[*]"
#master="spark://mercado-9:7077"
driver_memory=3g
executor_memory=0g
gc_threads=2
