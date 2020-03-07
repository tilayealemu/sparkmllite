package org.apache.spark.ml.classification

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

object Serialize {

  def writeObject(filename: String, obj: Serializable): Unit = {
    val fout = new FileOutputStream(filename)
    val oos = new ObjectOutputStream(fout)
    oos.writeObject(obj)
  }

  def readObject[T](filename: String): T = {
    val fin = new FileInputStream(filename)
    val ois = new ObjectInputStream(fin)
    ois.readObject().asInstanceOf[T]
  }
}
