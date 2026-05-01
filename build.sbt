ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.bahalla"

val sparkVersion = "3.5.8"

lazy val root = (project in file("."))
  .settings(
    name := "spark-kafka-extractor",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
      "org.apache.spark" %% "spark-sql"  % sparkVersion % Provided,
      // Bundle the Kafka connector in the fat jar — the runtime Spark image doesn't ship it.
      "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
      "com.github.scopt" %% "scopt"                % "4.1.0",
      "org.scalatest"    %% "scalatest"            % "3.2.20" % Test
    ),

    // Include `Provided` deps when running locally with `sbt run` / `sbt runMain`.
    Compile / run := Defaults
      .runTask(
        Compile / fullClasspath,
        Compile / run / mainClass,
        Compile / run / runner
      )
      .evaluated,
    Compile / runMain := Defaults
      .runMainTask(
        Compile / fullClasspath,
        Compile / run / runner
      )
      .evaluated,

    // Spark needs forked JVM with these flags on JDK 11+.
    fork := true,
    javaOptions ++= Seq(
      "-Dspark.master=local[*]",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.base/java.net=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
      "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
    ),

    // Fat jar settings for Dataproc submission.
    assembly / mainClass       := Some("com.bahalla.KafkaSearch"),
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", _ @_*)               => MergeStrategy.discard
      case "reference.conf"                          => MergeStrategy.concat
      case _                                         => MergeStrategy.first
    }
  )
