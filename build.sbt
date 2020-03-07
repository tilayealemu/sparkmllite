val sparkVersion = "2.4.4"

lazy val commonSettings = Seq(
  organization := "com.ainsightful",
  version := "1.0.0",
  scalaVersion := "2.11.12",
  resolvers += Resolver.mavenLocal
)

lazy val disablePublish = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val `sparkml-lite` = (project in file("sparkml-lite"))
  .settings(
    name := "com.ainsightful.sparkml-lite",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-mllib" % sparkVersion,
      // test
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.powermock" % "powermock-api-mockito" % "1.6.4" % Test,
      "junit" % "junit" % "4.12" % Test
    )
  )

lazy val `sample` = (project in file("sample"))
  .dependsOn(`sparkml-lite`)
  .settings(
    name := "sample",
    disablePublish,
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-streaming" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion
    )
  )

lazy val root = (project in file("."))
  .aggregate(`sparkml-lite`, `sample`)
  .settings(disablePublish :_*)

