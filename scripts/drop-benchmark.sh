#!/bin/bash
#
# Delete benchmark output from experiment directory

# title         drop-benchmark.sh
# author        Luis Mateos
# date          21-12-2015
# usage         /drop-benchmark.sh app-id
# notes

set -e

scriptdir="$(dirname $(readlink -f $0))"

# Experiment outputs dir
experiments_dir=$scriptdir/../experiments
# History log dir
eventlog_dir=$experiments_dir/history
# Workers log dir
workerlog_dir=$experiments_dir/work

function remove_experiment_log() {
    # Read app ID from experiment output file
    app_id=$(grep "Application ID" $1 | awk 'NF>1{print $NF}')
    # Remove experiment output
    rm -rf $workerlog_dir/$app_id
    rm -rf $eventlog_dir/$app_id
    rm $1
}

remove_experiment_log $1
