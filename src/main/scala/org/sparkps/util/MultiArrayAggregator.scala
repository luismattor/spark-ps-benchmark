package org.sparkps.util

class MultiArrayAggregator(
  array1: Array[Double],
  array2: Array[Double],
  array3: Array[Double]) extends Serializable {

    val coeffs1 = array1

    val coeffs2 = array2
    
    val accum = Array.ofDim[Double](coeffs1.length)

    def add(instance: Int): this.type = {
      val r = new java.util.Random(301214L);
      var i = 0
      val len = accum.length
      while (i < len) { 
        accum(i) += r.nextFloat
        i += 1
      }
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

