package org.apache.spark.ml.classification

import org.apache.spark.sql.types._

object TestSchema {

  val Target = StructField("target", DoubleType, false)

  val Features = Array(
    StructField("feature1", StringType, false),
    StructField("feature2", DoubleType, false),
    StructField("feature3", IntegerType, false),
    StructField("feature4", BooleanType, false)
  )

  val FeaturesSchema = StructType(Features)

  val Schema = StructType(Target +: Features)

  val NumHashedFeatures = math.pow(2, 10).toInt
}
