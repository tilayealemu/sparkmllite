package com.ainsightful.sparkml.lite

import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.feature.FeatureHasher
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class FeatureHasherLiteBigDatasetSuite extends FunSuite with MockitoSugar with BeforeAndAfter {

  private val Dataset = "src/test/resources/titanic.csv"

  private val Features = Array(
    StructField("PassengerId", IntegerType, true),
    StructField("Pclass", IntegerType, true),
    StructField("Name", StringType, true),
    StructField("Sex", StringType, true),
    StructField("Age", DoubleType, true),
    StructField("SibSp", IntegerType, true),
    StructField("Parch", IntegerType, true),
    StructField("Ticket", StringType, true),
    StructField("Fare", DoubleType, true),
    StructField("Cabin", StringType, true),
    StructField("Embarked", StringType, true)
  )
  private val Target = StructField("Survived", DoubleType, true)
  private val Schema = StructType(Target +: Features)
  private val PredictSchema = StructType(Features)

  test("FeatureHasher and FeatureHasherLite result in the same predictions - hash size 2^3") {
    runTest(math.pow(2, 3).toInt, Schema, Features, Target)
  }

  test("FeatureHasher and FeatureHasherLite result in the same predictions - hash size 2^10") {
    runTest(math.pow(2, 10).toInt, Schema, Features, Target)
  }

  test("FeatureHasher and FeatureHasherLite result in the same predictions - hash size 2^18") {
    runTest(math.pow(2, 18).toInt, Schema, Features, Target)
  }
  private def runTest(hashSize: Int,
                      schema: StructType,
                      features: Array[StructField],
                      target: StructField): Unit = {
    val (model, actualPredictions) = predictWithFeatureHasher(hashSize, schema, features, target)
    val expectedPredictions = predictWithFeatureHasherLite(model, hashSize)
    assertArray(expectedPredictions, actualPredictions)
  }

  private def predictWithFeatureHasher(hashSize: Int,
                                       schema: StructType,
                                       features: Array[StructField],
                                       target: StructField
                                      ): (LogisticRegressionModel, Array[Double]) = {
    val spark = SparkSession.builder().config("spark.master", "local[*]").getOrCreate()
    val df = spark.read.format("csv").option("header", true).schema(schema).load(Dataset)
    df.show(100, false)
    val hash = new FeatureHasher().setInputCols(features.map(_.name)).setOutputCol("features").setNumFeatures(hashSize)
    val df2 = hash.transform(df).select(Target.name, "features")
    val lr = new LogisticRegression().setLabelCol(Target.name)
    val model = lr.fit(df2)
    val df3 = model.transform(df2)
    val labels = df3.select("prediction").rdd.map(r => r(0)).collect().map(x => x.asInstanceOf[Double])
    (model, labels)
  }

  private def predictWithFeatureHasherLite(model: LogisticRegressionModel, hashSize: Int): Array[Double] = {
    val file = scala.io.Source.fromFile(Dataset)
    val lines = file.getLines.toList.tail // skip header row
    file.close()

    val hasher = new FeatureHasherLite(PredictSchema, hashSize, true)
    val keys = "PassengerId,Pclass,Name,Sex,Age,SibSp,Parch,Ticket,Fare,Cabin,Embarked".split(",")
    val predictions =
      lines.map(line => {
        // split by comma (careful around strings with commas inside them), then remove the first "Survived" column
        val values = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1).tail
        assert(values.length === keys.length)

        val featureSet = mutable.Map[String, Any]()
        featureSet.put("PassengerId", values(0).toInt)
        if (!values(1).isEmpty) featureSet.put("Pclass", values(1).toInt)
        if (!values(2).isEmpty) featureSet.put("Name", if (values(2).count(_ == '"') > 2) values(2) else values(2).drop(1).dropRight(1))
        if (!values(3).isEmpty) featureSet.put("Sex", values(3))
        if (!values(4).isEmpty) featureSet.put("Age", values(4).toDouble)
        if (!values(5).isEmpty) featureSet.put("SibSp", values(5).toInt)
        if (!values(6).isEmpty) featureSet.put("Parch", values(6).toInt)
        if (!values(7).isEmpty) featureSet.put("Ticket", values(7))
        if (!values(8).isEmpty) featureSet.put("Fare", values(8).toDouble)
        if (!values(9).isEmpty) featureSet.put("Cabin", values(9))
        if (!values(10).isEmpty) featureSet.put("Embarked", values(10))

        val input = hasher.hash(featureSet.toMap)
        model.predict(input)
      })
    predictions.toArray
  }

  private def assertArray(expected: Array[Double], actual: Array[Double]) = {
    assert(expected.length === actual.length)
    (expected zip actual).zipWithIndex.foreach { case (element, index) => {
      if (element._1 != element._2) println(s"Assertion failed on index $index")
    }
    }
    assert(expected === actual)
  }
}
