package org.sparkps.util

class MultiArrayAggregator( 
    array1: Array[Double],
    array2: Array[Double],
    array3: Array[Double]) {

        val accum = Array.ofDim[Double](array1.length)

        def add(instance: Int): this.type = {
            val r = new java.util.Random(301214L);
            val nFeatures = array1.length
            accum.zip(Array.fill(nFeatures)(r.nextFloat)).map(t => t._1 + t._2)
            this
        }

        def merge(other: MultiArrayAggregator): this.type = {
            var i = 0
            val len = accum.length
            val accumOther = other.accum
            while (i < len) { 
                accum(i) += accumOther(i)
                i += 1
            }
            this
        }

    }

