package org.apache.spark.ml.classification

import org.apache.spark.ml.linalg.Vectors
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class LRMWrapperSuite extends FunSuite with MockitoSugar with BeforeAndAfter {

  test("dot product calculation") {
    val coefficients = Array(1.0, 2.0, -3.0)
    val intercept = 10.0
    val model = mockModel(coefficients, intercept)
    val features = Array(10.0, 20.0, 30.0)

    val m1 = coefficients.zip(features).foldLeft(0.0)((product, elem) => product + elem._1 * elem._2) + intercept
    val m2 = 1.0 * 10.0 + 2.0 * 20.0 + -3.0 * 30.0 + 10.0
    assert(m1 === m2)

    val actual = new LRMWrapper(model).predictRaw(Vectors.dense(features))

    val expected = 1.0 / (1.0 + math.exp(-m1))
    assert(expected === actual)
  }

  test("equals") {
    val m1 = mock[LogisticRegressionModel]
    val m2 = mock[LogisticRegressionModel]

    assert(new LRMWrapper(m1).equals(new LRMWrapper(m1)))
    assert(!new LRMWrapper(m2).equals(new LRMWrapper(m1)))
  }

  test("hashCode") {
    val m1 = mock[LogisticRegressionModel]
    val m2 = mock[LogisticRegressionModel]

    assert(m1.hashCode() != m2.hashCode())
    assert(new LRMWrapper(m1).hashCode === m1.hashCode())
    assert(new LRMWrapper(m2).hashCode === m2.hashCode())
  }

  private def mockModel(coefficients: Array[Double], intercept: Double) = {
    val lr = mock[LogisticRegressionModel]
    when(lr.coefficients).thenReturn(Vectors.dense(coefficients))
    when(lr.intercept).thenReturn(intercept)
    lr
  }
}
