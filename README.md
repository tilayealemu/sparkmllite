SparkMLLite solves a specific problem in using Spark MLlib
models for prediction.

What problems?
- Usage of Spark MLlib models is dependent on having a Spark Session.
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

What's the solution?
This repo allows you to hash datapoints by giving it a Map instead of
a dataframe. You can then either pass this to MLlib's model for prediction.

What else is included?
`LogisticRegressionModel.predict` provides 0 or 1 values based on its learnt
threshold. If you need the raw scores or probabilities, you can use LRMLite.
