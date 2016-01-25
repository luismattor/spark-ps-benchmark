#!/usr/bin/python
#
# Parses PS experiment logging and prints basic statistics
#

import sys
import getopt

metrics = dict()

def usage():
    print("metrics-ps. -i <gc-stats-file>")

def compute_basic_stats(metric, mode="read-all"):
    if not metric in metrics:
        return "[NOT FOUND]"
    values = metrics[metric]
    avg = 0.0
    minimum = float('inf')
    maximum = 0.0
    if mode == "skip-first":
        values = values[1:]
    elif mode == "skip-last":
        values = values[0:-1]
    for v in values:
        minimum = min(minimum, v)
        maximum = max(maximum, v)
        avg += v
    avg = avg/len(values)
    return "%.6f, %.6f, %.6f [%d Found]" % (avg, minimum, maximum, len(values))

def print_master_stats(benchmark="generic"):
    print "[Driver statistics]"
    if benchmark == "ps":
        print "PS Get (total)            [s]: ", compute_basic_stats("ps-get", mode="skip-first")
        print "Broadcast                 [s]: ", compute_basic_stats("ps-broadcast")
        print "PS Job                    [s]: ", compute_basic_stats("ps-job")
        print "Update vector             [s]: ", compute_basic_stats("ps-vector-update", mode="skip-first")
        print "PS Get (trip)             [s]: ", compute_basic_stats("ps-net-get", mode="skip-last")
    print "Iteration time            [s]: ", compute_basic_stats("average")

def print_worker_stats(benchmark="generic"):
    print "[Executor statistics]" 
    if benchmark == "ps":
        print "Casting iter to array     [s]: ", compute_basic_stats("ps-toarray")
        print "Create client             [s]: ", compute_basic_stats("ps-create-client")
        print "Client function time      [s]: ", compute_basic_stats("ps-function")
        print "Create delta              [s]: ", compute_basic_stats("ps-delta")
        print "Stop client               [s]: ", compute_basic_stats("ps-stop-client")
        print "PS set (trip)             [s]: ", compute_basic_stats("ps-net-update", mode="skip-first")
        print "Unregister client (trip)  [s]: ", compute_basic_stats("ps-net-remove")

def parse_metrics_file(filename):
    f = open(filename, 'r')
    for line in f:
        if "[s]" in line:
            fields = line.strip().split("\t")
            metric=fields[0].split("[")[0].replace(" ", "")
            value=fields[1]
            #print metric, value
            if not metric in metrics:
                metrics[metric] = []
            metrics[metric].append(float(value))

def main(argv):
    try:
        opts, args = getopt.getopt(argv, "i:m:b:")
    except getopt.GetoptError as err:
        print(err)
        usage()
        sys.exit(2)

    input_file = None
    mode = "none"
    benchmark = "all"
    for opt, arg in opts:
        if opt == "-i":
            input_file = arg
        elif opt == "-m":
            mode = arg
        elif opt = "-b" :
            benchmark = arg
        else:
            assert False, "Unhandled exception"

    parse_metrics_file(input_file)
    if "master" in mode:
        print_master_stats(benchmark)
    elif "worker" in mode:
        print_worker_stats(benchmark)
   
if __name__ == '__main__':
    main(sys.argv[1:])
