#!/usr/bin/python
#
# Parses GC logging and prints basic statistics
#

import sys
import getopt

class MemoryTuple:
    
    def __init__(self, before, after):
        self.before=before
        self.after=after

    def __str__(self):
        return "(" + str(self.before) + "," + str(self.after) + ")"

class HeapTuple:

    def __init__(self):
        self.young = None
        self.old = None
        self.perm = None
        self.time = 0.0

    def __str__(self):
        return "Young: " + str(self.young) + " Old: " + str(self.old) + " Perm: " + str(self.perm) + " Time: " + str(self.time)

def usage():
    print("metrics-gc.py -i <gc-stats-file>")

def parse_gc_operation(line, generation):
    index_keyword = line.find(generation)
    index_arrow = line.find(">", index_keyword)

    start_before = index_keyword + len(generation) + 1
    end_before = index_arrow - 2
    start_after = index_arrow + 1
    end_after = line.find("K", index_arrow)

    before = line[start_before:end_before]
    after = line[start_after:end_after]

    return MemoryTuple(int(before), int(after))

def parse_gc_time(line):
    start=line.find("real")+5
    end=line.find(" ", start)
    return float(line[start:end])
    
def parse_major_gc_line(line):
    gc_tuple = HeapTuple()
    gc_tuple.young = parse_gc_operation(line, "PSYoungGen")
    gc_tuple.old = parse_gc_operation(line, "ParOldGen")
    gc_tuple.perm = parse_gc_operation(line, "PSPermGen")
    gc_tuple.time = parse_gc_time(line)
    return gc_tuple

def parse_minor_gc_line(line):
    gc_tuple = HeapTuple()
    gc_tuple.young = parse_gc_operation(line, "PSYoungGen")
    gc_tuple.time = parse_gc_time(line)
    return gc_tuple

def parse_metrics_file(file):
    f = open(file, 'r')
    major=[]
    minor=[]
    for line in f:
        cline=line.strip()
        if "[Full" in cline:
            major.append(parse_major_gc_line(cline))
        elif "[GC" in cline:
            minor.append(parse_minor_gc_line(cline))
    return major, minor

def get_gc_time_stats(gcs):
    if not gcs:
        return "[Empty]"
    avg = 0.0
    minimum = float('inf')
    maximum = 0.0
    for gc in gcs:
        minimum = min(minimum, gc.time)
        maximum = max(maximum, gc.time)
        avg += gc.time
    avg = avg/len(gcs)
    return "%.6f, %.6f, %.6f [%d Found]" % (avg, minimum, maximum, len(gcs))

def main(argv):
    try:
        opts, args = getopt.getopt(argv, "i:")
    except getopt.GetoptError as err:
        print(err)
        usage()
        sys.exit(2)

    input_file = None
    for opt, arg in opts:
        if opt == "-i":
            input_file = arg
        else:
            assert False, "Unhandled exception"

    major, minor = parse_metrics_file(input_file)
    print "major-gc: ", get_gc_time_stats(major)
    print "minor-gc: ", get_gc_time_stats(minor)

if __name__ == '__main__':
    main(sys.argv[1:])


