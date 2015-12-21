package org.sparkps

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.nio.ByteBuffer
import java.nio.ByteOrder

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

@RunWith(classOf[JUnitRunner])
class TachyonOperationsSuite extends FunSuite {
  
  val tfs:TachyonFileSystem = {
    val tachyonConf: TachyonConf = ClientContext.getConf()
    tachyonConf.set(Constants.MASTER_HOSTNAME, "localhost")
    tachyonConf.set(Constants.MASTER_PORT, "19998")
    ClientContext.reset(tachyonConf)
    TachyonFileSystem.TachyonFileSystemFactory.get()
  }
  
  test("Encoding array to tachyon") {
    val array:Array[Double] = Array.fill[Double](2)(0.1)
    array.foreach(println)
    writeTachyon("/array", array)
    val copy:Array[Double] = readTachyon("/array")
    copy.foreach(println)
  }
   
  private def writeTachyon(path: String, obj: Array[Double]): String = {
    val time: Long = System.nanoTime()
    try {
      val writeOptions = new OutStreamOptions.Builder(ClientContext.getConf()).setWriteType(WriteType.MUST_CACHE).setBlockSizeBytes(96000000).build()
      val uri: TachyonURI = new TachyonURI(path)
      val fos: FileOutStream = tfs.getOutStream(uri, writeOptions)
      val ba: Array[Byte] = encodeMessage(obj)
      fos.write(ba)
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
   
  private def readTachyon(path: String): Array[Double] = {
    val time: Long = System.nanoTime()
    var obj:Option[Array[Double]] = None:Option[Array[Double]]
    try {
      val readOptions = new InStreamOptions.Builder(ClientContext.getConf()).setReadType(ReadType.CACHE).build()
      val uri: TachyonURI = new TachyonURI(path)
      val file: TachyonFile = tfs.open(uri)
      val fis: FileInStream = tfs.getInStream(file, readOptions)
      val ba: Array[Byte] = new Array[Byte](2*8)
      fis.read(ba)
      obj = Some(decodeMessage(ba))
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
   
  private def encodeMessage(param:Array[Double]): Array[Byte] = {
    var array : Array[Byte] = new Array[Byte](param.length * 8);
    val buffer = ByteBuffer.wrap(array)
    //val buffer = ByteBuffer.wrap(array).order(Order)
    buffer.asDoubleBuffer().put(param)
    array
  }
  
  private def decodeMessage(param:Array[Byte]): Array[Double] = {
    var array : Array[Double] = new Array[Double](param.length / 8);
    val byteBuff = ByteBuffer.wrap(param)
    //val byteBuff = ByteBuffer.wrap(param).order(Order)
    byteBuff.asDoubleBuffer().get(array)
    array
  }
}
    
