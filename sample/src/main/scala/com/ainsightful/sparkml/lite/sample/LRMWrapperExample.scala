package com.ainsightful.sparkml.lite.sample

import java.io.{FileInputStream, ObjectInputStream}

import com.ainsightful.sparkml.lite.FeatureHasherLite
import org.apache.spark.ml.classification.{LRMWrapper, LogisticRegressionModel}

object LRMWrapperExample {

  def main(args: Array[String]): Unit = {
    // Define schema and hash size for training and prediction
    val predictSchema = Common.predictSchema
    val hashSize = Common.hashSize

    // load persisted model and wrap it
    val lrModel = new LRMWrapper(deserialize("lr.model").asInstanceOf[LogisticRegressionModel])

    // create hasher, schema must be exactly as it was used for training
    val hasher = new FeatureHasherLite(predictSchema, hashSize)

    // create sample data-point and hash it
    val feature = Map("feature1" -> "value1", "feature2" -> 2.0, "feature3" -> 3, "feature4" -> false)
    val featureVector = hasher.hash(feature)

    // Use LRMLite for raw prediction
    val prediction = lrModel.predictRaw(featureVector)
    println(s"Predicted value: $prediction")
  }

  private def deserialize(file: String) = {
    val fin = new FileInputStream(file)
    val ois = new ObjectInputStream(fin)
    ois.readObject()
  }
}
