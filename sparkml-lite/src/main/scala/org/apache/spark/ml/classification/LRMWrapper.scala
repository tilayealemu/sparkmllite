package org.apache.spark.ml.classification

import org.apache.spark.ml.linalg.{BLAS, Vector}

class LRMWrapper(val underlyingModel: LogisticRegressionModel) extends Serializable {

  def predict(features: Vector) = underlyingModel.predict(features)

  def predictRaw(features: Vector) = {
    val m = BLAS.dot(features, underlyingModel.coefficients) + underlyingModel.intercept
    1.0 / (1.0 + math.exp(-m))
  }

  override def equals(that: Any): Boolean =
    that match {
      case that: LRMWrapper => {
        that.underlyingModel.equals(underlyingModel)
      }
      case _ => false
    }

  override def hashCode: Int = underlyingModel.hashCode()
}
