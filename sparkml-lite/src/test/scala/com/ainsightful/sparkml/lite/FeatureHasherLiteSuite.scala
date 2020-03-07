package com.ainsightful.sparkml.lite

import org.apache.spark.ml.feature.FeatureHasher
import org.apache.spark.ml.linalg.{SparseVector, Vectors}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class FeatureHasherLiteSuite extends FunSuite with MockitoSugar with BeforeAndAfter {

  private val spark = SparkSession.builder().config("spark.master", "local[*]").getOrCreate()
  private val numberOfFeatures = Array(2, 4, 8, 16, 32)

  test("test small hash size to force collisions") {
    val schema = StructType(Array(
      StructField("col1", StringType, false),
      StructField("col2", StringType, false),
      StructField("col3", StringType, false),
      StructField("col4", StringType, false),
      StructField("col5", StringType, false)
    ))
    val df = makeDF(Array("1", "2", "3", "4", "5"), schema)

    // purposely use the same column name and values so that collision occurs
    val n = 2

    val hash = hashCFH(df, n, schema).asInstanceOf[SparseVector]

    // check hashes
    assert(0 === FeatureHasherLite.nonNegativeMod(FeatureHasherLite.hash("col1=1"), n))
    assert(0 === FeatureHasherLite.nonNegativeMod(FeatureHasherLite.hash("col2=2"), n))
    assert(0 === FeatureHasherLite.nonNegativeMod(FeatureHasherLite.hash("col3=3"), n))
    assert(1 === FeatureHasherLite.nonNegativeMod(FeatureHasherLite.hash("col4=4"), n))
    assert(1 === FeatureHasherLite.nonNegativeMod(FeatureHasherLite.hash("col5=5"), n))

    // Check assembled vector. Sparse vector has two arrays, one for indices and another for values
    // n=2, 3 values with 0 and 2 values with 1. On collision hash function should keep adding the counts.
    val expected = Vectors.sparse(2, Array(0, 1), Array(3, 2))
    assert(expected === hash)

    val hash2 = hashFH(df, n, schema)
    assert(hash === hash2)
  }

  test("test various data types") {
    val sampleSchema = StructType(Array(
      StructField("feature1", StringType, false),
      StructField("feature2", DoubleType, false),
      StructField("feature3", IntegerType, false),
      StructField("feature4", BooleanType, false)
    ))
    val sampleData = Array("test value 1", 2.456, 8863, true)
    val df = makeDF(sampleData, sampleSchema)
    compareHash(df, numberOfFeatures, StructType(sampleSchema.fields.tail))
  }

  test("test missing value for results in error if check is enabled") {
    val sampleSchema = StructType(Array(StructField("feature1", StringType, true)))
    val feature = Map("some_other_feature" -> "value")

    val hash1 = new FeatureHasherLite(sampleSchema, 2, false)
    hash1.hash(feature)

    val hash2 = new FeatureHasherLite(sampleSchema, 2, false)
    try {
      hash2.hash(feature)
    } catch {
      case e: RuntimeException => assert(e.getMessage === "Non-nullable column should have value: some_other_feature. Set checkNullable=false if the check is really not needed.")
      case x: Throwable => throw x
    }
  }

  def compareHash(df: DataFrame, n: Array[Int], schema: StructType) {
    n.foreach(m => {
      val v1 = hashFH(df, m, schema)
      val v2 = hashCFH(df, m, schema)
      assert(v1 === v2)
    })
  }

  private def hashFH(df: DataFrame, n: Int, schema: StructType) = {
    val hash = new FeatureHasher()
      .setInputCols(schema.map(_.name).toArray)
      .setOutputCol("features")
      .setNumFeatures(n)
    val firstRow = hash.transform(df).select("features").head(1)
    val firstCol = firstRow(0)(0)
    firstCol
  }

  private def hashCFH(df: DataFrame, n: Int, schema: StructType) = {
    val row: Row = df.first
    val features = row.getValuesMap[Any](row.schema.fieldNames)
    val hash = new FeatureHasherLite(schema, n)
    hash.hash(features)
  }

  private def makeDF(features: Array[Any], schema: StructType) = {
    val row = Row.fromSeq(features)
    val r = List(row).asJava
    val df = spark.createDataFrame(r, schema)
    df
  }
}
