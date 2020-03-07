package com.ainsightful.sparkml.lite.sample

import org.apache.spark.sql.types.{BooleanType, DoubleType, IntegerType, StringType, StructField, StructType}

object Common {
  val target = StructField("target", DoubleType, false)

  val features = Array(
    StructField("feature1", StringType, false),
    StructField("feature2", DoubleType, false),
    StructField("feature3", IntegerType, false),
    StructField("feature4", BooleanType, false)
  )
  val trainSchema = StructType(target +: features)

  val predictSchema = StructType(features)

  val hashSize = math.pow(2, 10).toInt
}
