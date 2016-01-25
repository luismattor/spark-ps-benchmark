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
#                   ZenPSUpdateBenchmark
#                   TachyonUpdateBenchmark
#                   TachyonMapUpdateBenchmark
# notes			Setup spark configuration for local and cluster mode according to benchmark environment

set -e

scriptdir="$(dirname $(readlink -f $0))"

. "$scriptdir/benchmark-env.sh"

# Script parameters
mode=$1
benchmark_class=$2
n_partitions=$3
n_iterations=$4
n_features=$5
benchmark_output=$6

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
        --conf spark.driver.maxResultSize=2048m \
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
