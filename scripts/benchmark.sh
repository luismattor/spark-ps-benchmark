#!/bin/bash
#
# Run Parameter Server Benchmark

# title         benchmark.sh
# author        Luis Mateos
# date          31-07-2015
# usage         /benchmark.sh (local|cluster) benchmark-class n-partitions n-iterations n-features output-file
#               Available benchmarks:
#                   SparkUpdateBenchmark
#                   PSUpdateBenchmark
#                   PSBroadcastUpdateBenchmark
#                   DistMLBenchmark
# notes			Setup spark configuration for local and cluster mode according to benchmark environment

set -e

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

# Default spark configuration (used for local mode)
master="local[*]"
driver_memory=3g
executor_memory=0g
gc_threads=2

# Spark configuration for cluster mode
if [ "$mode" == "cluster" ]
then
    master="spark://mercado-9:7077"
    driver_memory=14g
    executor_memory=14g
    gc_threads=4
fi

function run_benchmark() {
    echo "$(date) Running benchmark $benchmark_class in $mode mode"
    $spark_home/bin/spark-submit \
        --master $master \
        --name $benchmark_class \
        --class "org.sparkps.$benchmark_class" \
        --driver-memory $driver_memory \
        --executor-memory $executor_memory \
        --conf spark.eventLog.enabled="true" \
        --conf spark.eventLog.dir="$eventlog_dir" \
        --conf spark.driver.extraJavaOptions="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:ParallelGCThreads=$gc_threads" \
        --conf spark.executor.extraJavaOptions="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:ParallelGCThreads=$gc_threads" \
        "$benchmark_jar" $n_partitions $n_iterations $n_features > $experiments_dir/$benchmark_output
}

# Fecthes worker outputs from spark applications on cluster mode.
# Outputs stored on remote nodes will be moved to local node through ssh.
function fetch_remote_worker_logs() {
    # Read app ID from experiment output file
    app_id=$(grep "Application ID" $experiments_dir/$benchmark_output | awk 'NF>1{print $NF}')
    # Experiment log dir on remote nodes
    app_log_dir_remote=$workerlog_dir_remote/$app_id
    # Experiment log dir on local computer (output files will be moved to this directory)
    app_log_dir_local=$workerlog_dir/$app_id

    for node in "${nodes[@]}"
    do
        worker_ip=$worker_user@$node
        worker_id=$(ssh $worker_ip "ls $app_log_dir_remote")
        mkdir -p $app_log_dir_local/$node
        scp $worker_ip:$app_log_dir_remote/$worker_id/stdout $app_log_dir_local/$node/stdout > /dev/null
        scp $worker_ip:$app_log_dir_remote/$worker_id/stderr $app_log_dir_local/$node/stderr > /dev/null
    done
}

# Fecthes worker outputs from numa spark applications.
# Outputs stored on tmpfs are copied to disk
function fetch_numa_worker_logs() {
    # Read app ID from experiment output file
    app_id=$(grep "Application ID" $experiments_dir/$benchmark_output | awk 'NF>1{print $NF}')
    # Experiment log dir on local computer (output files will be moved to this directory)
    app_log_dir_local=$workerlog_dir/$app_id

    for node in "${nodes[@]}"
    do
        # Experiment log dir on numa node (experiment outputs are written into this directoy)
        app_log_dir_numa=$workerlog_dir_remote/store-$node/$app_id
        worker_id=$(ls $app_log_dir_numa)
        mkdir -p $app_log_dir_local/$node
        cp $app_log_dir_numa/$worker_id/stdout $app_log_dir_local/$node/stdout
        cp $app_log_dir_numa/$worker_id/stderr $app_log_dir_local/$node/stderr
    done
}

# Main

mkdir -p $eventlog_dir
mkdir -p $workerlog_dir

case $mode in 
    local) run_benchmark ;;
    cluster) run_benchmark && fetch_remote_worker_logs ;;
    *) echo "Unknown benchmark mode $mode" 
esac
