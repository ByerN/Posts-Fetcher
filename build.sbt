ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := "project-foo-1.jar",
    //Code below fixes problem with dependency merging on jar assemble (conflicts, missing Logback etc)
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs@_*) =>
        xs.map(_.toLowerCase) match {
          case "services" :: xs =>
            MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.discard
        }
      case PathList("reference.conf") => MergeStrategy.concat
      case x => MergeStrategy.first
    },
    name := "project-foo-1"
  )

ThisBuild / libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-client" % Versions.http4sVersion,
  "org.http4s" %% "http4s-circe" % Versions.http4sVersion,
  "org.http4s" %% "http4s-dsl" % Versions.http4sVersion,

  "io.circe" %% "circe-generic" % Versions.circeVersion,
  "io.circe" %% "circe-parser" % Versions.circeVersion,

  "co.fs2" %% "fs2-core" % Versions.fs2Version,
  "co.fs2" %% "fs2-io" % Versions.fs2Version,

  "org.typelevel" %% "cats-core" % Versions.catsVersion,
  "org.typelevel" %% "cats-effect" % Versions.catsEffectVersion,

  "com.typesafe" % "config" % Versions.typesafeConfigVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.typesafeScalaLoggingVersion,

  "ch.qos.logback" % "logback-classic" % Versions.logbackVersion,

  "org.scalamock" %% "scalamock" % Versions.scalaMockTestVersion % Test,
  "org.scalatest" %% "scalatest" % Versions.scalaTestVersion % Test,
)