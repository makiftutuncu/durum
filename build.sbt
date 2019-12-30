// === Dependencies ===

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0" % Test

// === Project Settings ===

description          in ThisBuild := "Delicious HTTP Wrapper"
homepage             in ThisBuild := Some(url("https://github.com/makiftutuncu/durum"))
startYear            in ThisBuild := Some(2019)
licenses             in ThisBuild := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
organization         in ThisBuild := "dev.akif"
organizationName     in ThisBuild := "Mehmet Akif Tütüncü"
organizationHomepage in ThisBuild := Some(url("https://akif.dev"))
developers           in ThisBuild := List(Developer("makiftutuncu", "Mehmet Akif Tütüncü", "m.akif.tutuncu@gmail.com", url("https://akif.dev")))
scmInfo              in ThisBuild := Some(ScmInfo(url("https://github.com/makiftutuncu/durum"), "git@github.com:makiftutuncu/durum.git"))

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",

  libraryDependencies ++= Seq(
    scalaTest
  )
)

// === Modules ===

lazy val root = project
  .in(file("."))
  .aggregate(`durum`)
  .settings(
    skip in publish := true
  )

lazy val durum = project
  .in(file("durum"))
  .settings(commonSettings)

// === Release Settings ===

import ReleaseTransformations._

credentials          in ThisBuild += Credentials(Path.userHome / ".sbt" / "sonatype_credential")
pomIncludeRepository in ThisBuild := { _ => false }
publishMavenStyle    in ThisBuild := true
publishTo            in ThisBuild := { Some(if (isSnapshot.value) "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" else "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") }

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("durum/publishLocal"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
