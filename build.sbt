import sbt.Keys.{organization, publishTo}
import sbt.url

val sparkVersion = "2.4.4"

lazy val commonSettings = Seq(
  organization := "com.ainsightful",
  version := "1.0.1",
  scalaVersion := "2.11.12",
  resolvers += Resolver.mavenLocal,
  publishTo := {
    if (isSnapshot.value) Some(Opts.resolver.sonatypeSnapshots)
    else Some(Opts.resolver.sonatypeStaging)
  },
  organization := "com.ainsightful",
  organizationName := "com.ainsightful",
  organizationHomepage := Some(url("http://ainsightful.com/")),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/tilayealemu/sparkmllite"),
      "scm:git@github.com:tilayealemu/sparkmllite.git"
    )
  ),
  developers := List(
    Developer(
      id = "Tilaye Yismaw Alemu",
      name = "Tilaye Yismaw Alemu",
      email = "tilaye@gmail.com",
      url = url("http://ainsightful.com")
    )
  ),

  description := "Spark MLlib helper library",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/tilayealemu/sparkmllite")),

  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ => false },
  publishTo := {
    if (isSnapshot.value) Some(Opts.resolver.sonatypeSnapshots)
    else Some(Opts.resolver.sonatypeStaging)
  },
  publishMavenStyle := true
)

lazy val disablePublish = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Dummy", file("dummy")))
)

lazy val `sparkml-lite` = (project in file("sparkml-lite"))
  .settings(
    name := "sparkml-lite",
    commonSettings,
    publishArtifact := true,
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
  .settings(
    name := "sample",
    disablePublish,
    publishArtifact := false,
    commonSettings,
    libraryDependencies ++= Seq(
      "com.ainsightful" %% "sparkml-lite" % "1.0.0",
      "org.apache.spark" %% "spark-streaming" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion
    )
  )

lazy val root = (project in file("."))
  .aggregate(`sparkml-lite`, `sample`)
  .settings(disablePublish: _*)
