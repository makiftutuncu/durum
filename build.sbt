import sbt.Keys._

// === Dependencies ===

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0" % Test

// === Project Settings ===

description          in ThisBuild := "Delicious HTTP Wrapper"
version              in ThisBuild := "1.0.0"
homepage             in ThisBuild := Some(url("https://github.com/makiftutuncu/durum"))
startYear            in ThisBuild := Some(2020)
licenses             in ThisBuild := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
organization         in ThisBuild := "dev.akif"
organizationName     in ThisBuild := "Mehmet Akif Tütüncü"
organizationHomepage in ThisBuild := Some(url("https://akif.dev"))
developers           in ThisBuild := List(Developer("makiftutuncu", "Mehmet Akif Tütüncü", "m.akif.tutuncu@gmail.com", url("https://akif.dev")))
scmInfo              in ThisBuild := Some(ScmInfo(url("https://github.com/makiftutuncu/durum"), "git@github.com:makiftutuncu/durum.git"))

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  javacOptions ++= Seq("-source", "11"),

  libraryDependencies ++= Seq(
    scalaTest
  ),

  apiMappings ++= {
    val classpath = (fullClasspath in Compile).value
    def findJar(name: String): File = {
      val regex = ("/" + name + "[^/]*.jar$").r
      classpath.find { jar => regex.findFirstIn(jar.data.toString).nonEmpty }.get.data
    }

    Map(
      findJar("scala-library") -> url(s"http://scala-lang.org/api/${scalaVersion.value}/")
    )
  },
)

// === Modules ===

lazy val durum = project
  .in(file("."))
  .aggregate(`durum-core`)
  .settings(
    skip in publish := true
  )

lazy val `durum-core` = project
  .in(file("durum-core"))
  .settings(commonSettings)
