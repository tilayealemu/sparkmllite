package com.ainsightful.sparkml.lite.sample

import java.io.{FileOutputStream, ObjectOutputStream}

import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.feature.FeatureHasher
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

object TrainExample {

  def main(args: Array[String]): Unit = {
    // Define schema and hash size for training and prediction
    val target = Common.target
    val features = Common.features
    val trainSchema = Common.trainSchema
    val hashSize = Common.hashSize

    // train and persist
    val lrModel = train("sample_data.csv", trainSchema, target, features, hashSize)
    serialize(lrModel, "lr.model")
  }

  def train(file: String, schema: StructType, target: StructField, features: Array[StructField], hashSize: Int): LogisticRegressionModel = {
    val spark = SparkSession.builder().config("spark.master", "local[*]").getOrCreate()
    println(s"Creating dataframe from $file")

    val df = spark.read
      .format("csv")
      .option("header", true)
      .schema(schema)
      .load(file)

    df.show()

    println("One hot encoding")
    val hash = new FeatureHasher()
      .setInputCols(features.map(_.name))
      .setOutputCol("features")
      .setNumFeatures(hashSize)
    val df2 = hash.transform(df).select(target.name, "features")

    println("Training logistic model")
    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)
      .setLabelCol(target.name)
    val lrModel = lr.fit(df2)

    lrModel
  }

  private def serialize(obj: Serializable, file: String) = {
    val fout = new FileOutputStream(file)
    val oos = new ObjectOutputStream(fout)
    oos.writeObject(obj)
  }
}
