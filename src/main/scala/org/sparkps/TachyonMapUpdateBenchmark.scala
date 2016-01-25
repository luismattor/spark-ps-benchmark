package org.sparkps

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import tachyon.Constants
import tachyon.TachyonURI
import tachyon.client.ClientContext
import tachyon.client.ReadType
import tachyon.client.WriteType
import tachyon.client.file.FileInStream
import tachyon.client.file.FileOutStream
import tachyon.client.file.TachyonFile
import tachyon.client.file.TachyonFileSystem
import tachyon.client.file.options.CreateOptions
import tachyon.client.file.options.InStreamOptions
import tachyon.client.file.options.OutStreamOptions
import tachyon.conf.TachyonConf
import tachyon.exception.TachyonException
import tachyon.util.CommonUtils
import tachyon.util.FormatUtils
    
import java.io.IOException
import java.io.ObjectOutputStream
import java.io.ObjectInputStream

object TachyonMapUpdateBenchmark {

    val ApplicationName = "Spark - Parameter Update Benchmark over tachyon"

    val tfs:TachyonFileSystem = createTachyonContext()

    def createTachyonContext(): TachyonFileSystem = {
        val tachyonConf: TachyonConf = ClientContext.getConf()
        tachyonConf.set(Constants.MASTER_HOSTNAME, "localhost")
        tachyonConf.set(Constants.MASTER_PORT, "19998")
        ClientContext.reset(tachyonConf)
        TachyonFileSystem.TachyonFileSystemFactory.get()
    }

    def writeTachyon(path: String, obj: Array[Double]): String = { 
        val time: Long = System.nanoTime()
        try {
            val writeOptions = new OutStreamOptions.Builder(ClientContext.getConf()).setWriteType(WriteType.MUST_CACHE).setBlockSizeBytes(96000000).build()
            val uri: TachyonURI = new TachyonURI(path)
            val fos: FileOutStream = tfs.getOutStream(uri, writeOptions)
            val oos: ObjectOutputStream = new ObjectOutputStream(fos)
            oos.writeObject(obj)
            oos.close()
            fos.close()
        } catch {
            case e:Exception => {
            println("Tachyon write exception: " + e)
            e.printStackTrace()
            }
        }
        println("Tachyon write [%s] [%f]".format(path, (System.nanoTime() - time) / 1e9))
        path
    }

    def readTachyon(path: String): Array[Double] = {
        val time: Long = System.nanoTime()
        var obj:Option[Array[Double]] = None:Option[Array[Double]]
        try {
            val readOptions = new InStreamOptions.Builder(ClientContext.getConf()).setReadType(ReadType.CACHE).build()
            val uri: TachyonURI = new TachyonURI(path)
            val file: TachyonFile = tfs.open(uri)
            val fis: FileInStream = tfs.getInStream(file, readOptions)
            val ois: ObjectInputStream = new ObjectInputStream(fis)
            obj = Some(ois.readObject().asInstanceOf[Array[Double]])
            ois.close()
            fis.close()
            tfs.delete(file)
        } catch {
            case e:Exception => {
                println("Tachyon read exception: " + e)
            }
        }
        println("Tachyon read [%s] [%f]".format(path, (System.nanoTime() - time) / 1e9))
        obj.get
    }

    def main(args: Array[String]) {

        val NumPartitions = args(0).toInt

        val NumIterations = args(1).toInt

        val NumFeatures = args(2).toInt

        val start = System.nanoTime 

        val conf = new SparkConf().setAppName(ApplicationName)

        val sc = new SparkContext(conf)

        println("------------------------------")
        println("Application ID:\t%s".format(sc.applicationId))
        println("Experiment.\tnp:\t%d\tnf:\t%d\tni:\t%d".format(NumPartitions, NumFeatures, NumIterations))

        val data = sc.parallelize(0 until NumPartitions, NumPartitions)
        //data.cache()
        var weights = Array.fill[Double](NumFeatures)(0.0)
        val time = System.nanoTime()
        for (i <- 0 until NumIterations) {
            val bcWeights = data.context.broadcast(weights)
            // Mapping
            data.mapPartitionsWithIndex( (i, iter) => {
                val w = Array.fill[Double](NumFeatures)(0.0)
                val r = new java.util.Random(301214L)
                val update = w.zip(Array.fill[Double](NumFeatures)(r.nextFloat)).map(t => t._1 + t._2)
                val file = "/reduce1-" + i
                //println("Map-out: " + update.sum)
                writeTachyon(file, update)
                Iterator()
            }).count()
            // First reduce
            val depth = 2
            val scale = math.max(math.ceil(math.pow(NumPartitions, 1.0 / depth)).toInt, 2)
            val numReducers = NumPartitions/scale
            val reduceIds = sc.parallelize(0 until numReducers, numReducers)
            reduceIds.mapPartitionsWithIndex( (i, iter) => {
                val batchSize = NumPartitions / numReducers
                val start = i * batchSize

                // Fix uneven batch sizes
                var end = start + batchSize
                if(NumPartitions - end < batchSize)
                    end = NumPartitions

                var inFile = "/reduce1-" + start
                var update = readTachyon(inFile)
                for (j <- (start + 1) until end) {
                    inFile = "/reduce1-" + j
                    val tmp = readTachyon(inFile)
                    update = update.zip(tmp).map(t => t._1 + t._2)
                }
                //println("Map-out-2: " + update.sum)
                val outFile = "/reduce2-" + i
                writeTachyon(outFile, update)
                Iterator()
            }).count()
            // Second reduce
            var inFile = "/reduce2-" + 0
            var update = readTachyon(inFile)
            for (j <- 1 until numReducers) {
                val inFile = "/reduce2-" + j
                val tmp = readTachyon(inFile)
                update = update.zip(tmp).map(t => t._1 + t._2)
            }
            weights = weights.zip(update).map(t => t._1 + t._2)
        }
        
        println("average[s]    :\t%f".format((System.nanoTime() - time) / 1e9 / NumIterations))
        val sum1 = weights.sum
        val r = new java.util.Random(301214L)
        val sum2 = NumIterations * NumPartitions * Array.fill[Double](NumFeatures)(r.nextFloat).sum
        println("sum [computed]:\t%f".format(sum1))
        println("sum [real]    :\t%f".format(sum2))
        println("total [s]     :\t%f".format((System.nanoTime - start) / 1e9))
        assert(Math.abs(sum1 - sum2) < 0.001, "Parameter Server Error")
    }
}

