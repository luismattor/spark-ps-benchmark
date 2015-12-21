#!/bin/bash
#
# Print stats about Parameter Server benchmark experiments
#
# title         stats.sh
# author        Luis Mateos
# date          11-08-2015
# usage         ./update-stats.sh experiment-file
# notes         Ensure experiment files generated by benchmark are available on experiment dir

set -e

scriptdir="$(dirname $(readlink -f $0))"

# Experiment log dir
experiments_dir=$scriptdir/../experiments
# Worker nodes for fetching logs
declare -a nodes=("node-0" "node-1" "node-2" "node-3" "node-4")

function compute_stats_cluster() {
    # Create new accumulated metrics file
    acc_file=stats.log
    if [ -s $acc_file ]; then
        rm $acc_file
    fi
    # Appending each worker output to accumulated metrics file
    for node in "${nodes[@]}"
    do
        node_file=$app_log_dir/$node/stdout
        if [ -s $node_file ]; then
            echo "$(date) Appending file for node " $node
            echo "$(date) Printing stats for node " $node
            cat $node_file >> $acc_file
            python metrics-ps.py -i $node_file -m worker
            python metrics-gc.py -i $node_file
        fi
    done
    python $scriptdir/metrics-ps.py -i $acc_file -m worker
    python $scriptdir/metrics-ps.py -i $1 -m master
    python $scriptdir/metrics-gc.py -i $acc_file
    python $scriptdir/metrics-gc.py -i $1
}

function compute_stats_local() {
    python $scriptdir/metrics-ps.py -i $1 -m master
    python $scriptdir/metrics-ps.py -i $1 -m worker
    python $scriptdir/metrics-gc.py -i $1

}

# Main

# Read app ID from experiment file
app_id=$(grep "Application ID" $1 | awk 'NF>1{print $NF}')

# Experiment log directory
app_log_dir=$experiments_dir/work/$app_id

echo "$(date) Computing benchmark experiments for $1 ($app_id)"

if [[ $app_id = local* ]]; then
    compute_stats_local $1
else
    compute_stats_cluster $1
fi

