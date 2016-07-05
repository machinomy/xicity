import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.Version

name := "xicity"

scalaVersion := "2.11.8"

organization := "com.machinomy"

resolvers ++= Seq(
  "tomp2p.net" at "http://tomp2p.net/dev/mvn/"
)

libraryDependencies ++= Seq(
  "org.scodec" %% "scodec-core" % "1.9.0",
  "com.github.jnr" % "jnr-ffi" % "2.0.9",
  "com.typesafe.akka" %% "akka-actor" % "2.4.5",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.5",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.5",
  "com.github.nscala-time" %% "nscala-time" % "2.10.0",
  "io.argonaut" %% "argonaut" % "6.1",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "net.tomp2p" % "tomp2p-all" % "5.0-Beta8",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.github.nscala-time" %% "nscala-time" % "2.10.0",
  "org.scodec" %% "scodec-core" % "1.9.0",
  "com.lihaoyi" % "upickle_2.11" % "0.4.0",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

def doIfNotSnapshot(step: ReleaseStep) = {
  ReleaseStep(
    action = st => {
      if (!st.get(versions).getOrElse((None, None))._1.toString.endsWith("-SNAPSHOT")) {
        step.action(st)
      }
      st
    },
    check = step.check,
    enableCrossBuild = step.enableCrossBuild
  )
}

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  doIfNotSnapshot(setReleaseVersion),
  doIfNotSnapshot(commitReleaseVersion),
  doIfNotSnapshot(tagRelease),
  publishArtifacts,
  doIfNotSnapshot(setNextVersion),
  doIfNotSnapshot(commitNextVersion),
  doIfNotSnapshot(pushChanges)
)

publishTo := {
  if (isSnapshot.value)
    Some("Machinomy" at "http://machinomy.com:8081/artifactory/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  else
    Some("Machinomy" at "http://machinomy.com:8081/artifactory/libs-release-local/")
}
credentials += Credentials(new File("credentials.properties"))