import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import ammonite.ops._
import coursier.Cache
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import mill.define.Target
import mill.util.Loose

val ScalaVersions = Seq("2.11.11", "2.12.6")

object library {

  object Version {
    val circe = "0.9.3"
    val cats = "1.1.0"

    val rosHttp = "2.2.0"
    val sttp = "1.2.0-RC6"
    val slogging = "0.6.1"

    val scalaTest = "3.0.5"
    val scalaMock = "3.6.0"
    val scalaMockScalaTest = "3.6.0"
    val scalaLogging = "3.8.0"
    val logback = "1.2.3"
    val scalajHttp = "2.4.0"

    val akkaVersion = "2.5.13"
    val akkaActor = akkaVersion
    val akkaStream = akkaVersion
    val akkaHttp = "10.1.3"
    val akkaHttpCors = "0.3.0"
    val hammock = "0.8.4"
  }

  val akkaHttp = ivy"com.typesafe.akka::akka-http::${Version.akkaHttp}"
  val akkaHttpTestkit = ivy"com.typesafe.akka::akka-http-testkit::${Version.akkaHttp}"

  val akkaActor = ivy"com.typesafe.akka::akka-actor::${Version.akkaActor}"
  val akkaStream = ivy"com.typesafe.akka::akka-stream::${Version.akkaStream}"

  val scalajHttp = ivy"org.scalaj::scalaj-http::${Version.scalajHttp}"
  val scalaLogging = ivy"com.typesafe.scala-logging::scala-logging::${Version.scalaLogging}"

  val scalaMockScalaTest = ivy"org.scalamock::scalamock-scalatest-support::${Version.scalaMockScalaTest}"
  val akkaHttpCors = ivy"ch.megard::akka-http-cors::${Version.akkaHttpCors}"
  val scalaTest = ivy"org.scalatest::scalatest::${Version.scalaTest}"
  val logback = ivy"ch.qos.logback:logback-classic::${Version.logback}"

  val circeCore = ivy"io.circe::circe-core::${Version.circe}"
  val circeGeneric = ivy"io.circe::circe-generic::${Version.circe}"

  val circeGenericExtras = ivy"io.circe::circe-generic-extras::${Version.circe}"
  val circeParser = ivy"io.circe::circe-parser::${Version.circe}"
  val circeLiteral = ivy"io.circe::circe-literal::${Version.circe}"

  val catsCore = ivy"org.typelevel::cats-core::${Version.cats}"
  val catsFree = ivy"org.typelevel::cats-free::${Version.cats}"

  val rosHttp = ivy"fr.hmil::roshttp::${Version.rosHttp}"

  val sttpCore = ivy"com.softwaremill.sttp::core::${Version.sttp}"
  val sttpCirce = ivy"com.softwaremill.sttp::circe::${Version.sttp}"
  val sttpOkHttp = ivy"com.softwaremill.sttp::okhttp-backend::${Version.sttp}"

  val slogging = ivy"biz.enef::slogging::${Version.slogging}"

  val hammock = ivy"com.pepegar::hammock-core::${Version.hammock}"
}

trait TelegramBot4sModule extends CrossScalaModule {

  override def scalacOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    //"-Xfatal-warnings",
    "-Xlint:_",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ypartial-unification"
  )

  override def ivyDeps = Agg(
    library.circeCore,
    library.circeGeneric,
    library.circeGenericExtras,
    library.circeParser,
    library.circeLiteral,
    library.catsCore,
    library.catsFree,
    library.sttpCore,
    library.slogging
  )

  trait Tests extends super.Tests {
    override def ivyDeps = Agg(
      library.scalaTest,
      library.scalaMockScalaTest,
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}

abstract class TelegramBot4sCrossPlatform(val platformSegment: String, location: String) extends TelegramBot4sModule {

  def millSourcePath = build.millSourcePath / location

  override def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platformSegment"
  )

  def crossScalaVersion: String

  override def artifactName = s"telegrambot4s-$location"
}

trait Publishable extends PublishModule {

  override def publishVersion = "3.3.0-RC2"

  def pomSettings = PomSettings(
    description = "Telegram Bot API wrapper for Scala",
    organization = "info.mukel",
    url = "https://github.com/mukel/telegrambot4s",
    licenses = Seq(License.Common.Apache2),
    versionControl = VersionControl.github("mukel", "telegrambot4s"),
    developers = Seq(
      Developer("mukel", "Alfonso² Peterssen", "https://github.com/mukel")
    )
  )
}

abstract class TelegramBot4sCore(platformSegment: String) extends TelegramBot4sCrossPlatform(platformSegment, "core")

object core extends Module {

  object jvm extends Cross[CoreJvmModule](ScalaVersions: _ *)

  class CoreJvmModule(val crossScalaVersion: String) extends TelegramBot4sCore("jvm") with Publishable {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      library.scalajHttp,
      library.sttpOkHttp
    )

    object test extends Tests

  }

  object js extends Cross[CoreJsModule](ScalaVersions: _ *)

  class CoreJsModule(val crossScalaVersion: String) extends TelegramBot4sCore("js")
    with ScalaJSModule with Publishable {

    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"io.scalajs.npm::node-fetch::0.4.2"
    )

    def scalaJSVersion = "0.6.23"

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    //object test extends Tests
  }

}


object akka extends Cross[AkkaModule](ScalaVersions: _ *)

class AkkaModule(val crossScalaVersion: String) extends TelegramBot4sModule with Publishable {
  override def artifactName = "telegrambot4s-akka"

  override def moduleDeps = Seq(core.jvm())

  override def ivyDeps = Agg(
    library.akkaActor,
    library.akkaHttp,
    library.akkaStream
  )

  object test extends Tests {
    override def moduleDeps = super.moduleDeps ++ Seq(core.jvm().test)

    override def ivyDeps = Agg(
      library.akkaHttpTestkit
    )
  }

}

abstract class TelegramBot4sExamples(platformSegment: String) extends TelegramBot4sCrossPlatform(platformSegment, "examples")

object examples extends Module {

  object jvm extends Cross[ExamplesJvmModule](ScalaVersions: _ *)

  class ExamplesJvmModule(val crossScalaVersion: String) extends TelegramBot4sExamples("jvm") {
    override def moduleDeps = Seq(core.jvm(), akka())

    override def ivyDeps = super.ivyDeps() ++ Agg(
      library.scalajHttp,
      library.akkaHttpCors,
      library.sttpOkHttp
    )
  }

  object js extends Cross[ExamplesJsModule](ScalaVersions: _ *)

  class ExamplesJsModule(val crossScalaVersion: String) extends TelegramBot4sExamples("js") with ScalaJSModule {
    override def moduleDeps = Seq(core.js())

    def scalaJSVersion = "0.6.23"

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}