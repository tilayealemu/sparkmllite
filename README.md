[![Build Status](https://travis-ci.org/tilayealemu/sparkmllite.svg?branch=master)](https://travis-ci.org/tilayealemu/sparkmllite)

# Background

SparkMLLite solves a specific problem in using Spark MLlib
models for prediction.

Usage of Spark MLlib models is dependent on having a Spark Session.
The first option is to start it locally (spark.master=local). Given 
spark pulls all sorts of dependencies, adding it as a dependency for 
making predictions is a bad idea. Also, it takes up un-necessary 
resources  at runtime.
- The second option is to use a spark master. This is also a waste
of resource.
- Yet another problem is prediction latency. Making predictions
may require one-hot encoding of features, typically through
`org.apache.spark.ml.feature.FeatureHasher`. Using it during prediction 
time requires constructing a dataframe. But there are scenarios where
components receive thousands and millions of requests per second and they
need to make single predictions, as opposed to offline batch predictions.
Such serving components can have SLAs of 1 millisecond or less. Creating
a dataframe for every single call makes this virtually unachievable.

## SparkMLLite

This repo allows you to hash datapoints by giving it a Map instead of
a dataframe. You can then either pass this to MLlib's model for prediction.

In addition, `LogisticRegressionModel.predict` provides 0 or 1 values based 
on its learnt threshold. If you need the raw scores or probabilities, 
you can use the provided `LRMLite` wrapper for LogisticRegressionModel.

# Usage

Sbt:

    libraryDependencies += "com.ainsightful" %% "sparkml-lite" % "1.0.1"

Maven:

    <dependency>
        <groupId>com.ainsightful</groupId>
        <artifactId>sparkml-lite</artifactId>
        <version>1.0.1</version>
    </dependency>


## Feature hashing

First use MLlib's [FeatureHasher](https://spark.apache.org/docs/2.4.4/api/java/org/apache/spark/ml/feature/FeatureHasher.html)
to hash feature values and train your model. Then save your model.
See `TrainExample` class under sample directory. For prediction,
use FeatureHasherLite with no need to start a spark session.

    val lrModel = ... // load saved model

    // create hasher, schema must be exactly as it was used for training
    val hasher = new FeatureHasherLite(predictSchema, hashSize)

    // create sample data-point and hash it
    val feature = Map("feature1" -> "value1", "feature2" -> 2.0, "feature3" -> 3, "feature4" -> false)
    val featureVector = hasher.hash(feature)

    // Make prediction
    val prediction = lrModel.predict(featureVector)

See `TrainExample` and `FeatureHasherLiteExample` for working examples.

## Raw predictions
To get LogistRegressionModel predict probabilities:

    // get trained model
    val lrModel = ...


    // prepare feature vector
    val input = ...
    
    // wrap model
    val lrModel2 = new LRMWrapper(model)
    
    // Get raw prediction. This value should have the raw prediction
    val prediction = lrModel2.predictRaw(input)
