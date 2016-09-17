import de.heikoseeberger.sbtheader.license.Apache2_0
import sbtrelease.ReleaseStateTransformations._

name := "xicity"

version := "0.0.5"

scalaVersion := "2.11.8"

organization := "com.machinomy"

mainClass := Some("com.machinomy.xicity.examples.SeedApp")

libraryDependencies ++= Seq(
  "org.scodec" %% "scodec-core" % "1.9.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.github.nscala-time" %% "nscala-time" % "2.10.0",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.github.scopt" %% "scopt" % "3.5.0"
)

releaseUseGlobalVersion := false

def whenRelease(releaseStep: ReleaseStep): ReleaseStep =
  releaseStep.copy(state => if (Project.extract(state).get(isSnapshot)) state else releaseStep.action(state))

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  runClean,
  runTest,
  whenRelease(tagRelease),
  publishArtifacts,
  whenRelease(pushChanges)
)

publishTo := {
  val base = "http://artifactory.machinomy.com/artifactory"
  if (isSnapshot.value) {
    val timestamp = new java.util.Date().getTime
    Some("Machinomy" at s"$base/snapshot;build.timestamp=$timestamp")
  } else {
    Some("Machinomy" at s"$base/release")
  }
}

credentials += Credentials(new File("credentials.properties"))

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

headers := Map(
  "scala" -> Apache2_0("2016", "Machinomy")
)

autoAPIMappings := true

scalacOptions := Seq("-feature")

scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits")
