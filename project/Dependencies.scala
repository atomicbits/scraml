
import sbt._

trait Dependencies { this:Build =>

  // main dependencies
  val scalaLogging    =   "com.typesafe.scala-logging"  %%    "scala-logging"       % "3.1.0"
  val logback         =   "ch.qos.logback"              %     "logback-classic"     % "1.1.1"

  val rxHttpClient    =   "be.wegenenverkeer"           %%    "rxhttpclient-scala"  % "0.1.0-SNAPSHOT"
  val playJson        =   "com.typesafe.play"           %%    "play-json"           % "2.3.7"

  val scalaReflect    =   "org.scala-lang"              %     "scala-reflect"       % "2.11.6"

  val ramlJavaParser  =   "org.raml"                    %     "raml-parser"         % "0.9-SNAPSHOT"


  // test dependencies
  val scalaTest       =   "org.scalatest"               %%    "scalatest"           % "2.2.4"    % "test"
  val wiremock        =   "com.github.tomakehurst"      %     "wiremock"            % "1.55"     % "test"

  val scramlgenTestCode = "io.atomicbits"               %%    "scraml-testdef"      % "0.1.0-SNAPSHOT"



  // inclusion of the above dependencies in the modules
  val scramlGeneratorDeps = Seq (
    scalaReflect,
    rxHttpClient,
    playJson,
    wiremock
  )

  val scramlTestDeps = Seq (
    scramlgenTestCode
  )

  val scramlParserDeps = Seq(
    ramlJavaParser
  )

  val mainDeps = Seq(
    scalaLogging,
    logback
  )

  val testDeps = Seq(
    scalaTest
  )

  val allDeps = mainDeps ++ testDeps

}
