ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "localstack-error",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "3.0.4",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.19",
      "com.typesafe.akka" %% "akka-slf4j" % "2.6.19",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "org.scalatest" %% "scalatest" % "3.2.13",
      "com.dimafeng" %% "testcontainers-scala-core" % "0.40.10"
    ).map(_ % Test)
  )
