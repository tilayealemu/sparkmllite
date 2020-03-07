package com.ainsightful.sparkml.lite

import org.apache.spark.SparkException
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.types.{DoubleType, NumericType, StructType}
import org.apache.spark.unsafe.hash.Murmur3_x86_32.{hashInt, hashLong, hashUnsafeBytes2}
import org.apache.spark.unsafe.types.UTF8String

import scala.collection.mutable

class FeatureHasherLite(schema: StructType, hashSize: Int, checkNullable: Boolean = true) {

  private val columns = schema.map(struct => struct.name -> struct).toMap

  private val realFields = schema.fields.filter(_.dataType.isInstanceOf[NumericType]).map(_.name).toSet
  private val dataTypes = schema.fields.map(x => x.name -> x.dataType).toMap

  def hash(feature: Map[String, Any]) = {
    val map = new mutable.OpenHashMap[Int, Double]()
    columns.keys.foreach({ colName =>
      if (feature.contains(colName)) {
        val (rawIdx, value) =
          if (realFields(colName)) {
            // numeric values are kept as is, with vector index based on hash of "column_name"
            val value =
              if (dataTypes(colName) == DoubleType) feature(colName).asInstanceOf[Double]
              else feature(colName).toString.toDouble
            val hash = FeatureHasherLite.hash(colName)
            (hash, value)
          } else {
            // string, boolean and numeric values that are in catCols are treated as categorical,
            // with an indicator value of 1.0 and vector index based on hash of "column_name=value"
            val value = feature(colName).toString
            val fieldName = s"$colName=$value"
            val hash = FeatureHasherLite.hash(fieldName)
            (hash, 1.0)
          }
        val idx = FeatureHasherLite.nonNegativeMod(rawIdx, hashSize)
        map.put(idx, map.get(idx).getOrElse(0.0) + value)
      } else if (checkNullable && !columns(colName).nullable) {
        throw new RuntimeException(s"Non-nullable column should have value: ${colName}. Set checkNullable=false if the check is really not needed.")
      }
    })
    Vectors.sparse(hashSize, map.toSeq)
  }
}

object FeatureHasherLite {

  private val seed = 42

  def hash(term: Any): Int = {
    term match {
      case null => seed
      case b: Boolean => hashInt(if (b) 1 else 0, seed)
      case b: Byte => hashInt(b, seed)
      case s: Short => hashInt(s, seed)
      case i: Int => hashInt(i, seed)
      case l: Long => hashLong(l, seed)
      case f: Float => hashInt(java.lang.Float.floatToIntBits(f), seed)
      case d: Double => hashLong(java.lang.Double.doubleToLongBits(d), seed)
      case s: String =>
        val utf8 = UTF8String.fromString(s)
        hashUnsafeBytes2(utf8.getBaseObject, utf8.getBaseOffset, utf8.numBytes(), seed)
      case _ => throw new SparkException("FeatureHasher with murmur3 algorithm does not " +
        s"support type ${term.getClass.getCanonicalName} of input data.")
    }
  }

  /* Calculates 'x' modulo 'mod', takes to consideration sign of x,
   * i.e. if 'x' is negative, than 'x' % 'mod' is negative too
   * so function return (x % mod) + mod in that case.
   */
  def nonNegativeMod(x: Int, mod: Int): Int = {
    val rawMod = x % mod
    rawMod + (if (rawMod < 0) mod else 0)
  }
}
