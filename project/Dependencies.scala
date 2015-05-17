
import sbt._

trait Dependencies { this:Build =>

  val macwireMacros   =   "com.softwaremill.macwire"    %%    "macros"            % "1.0.1"
  val macwireRuntime  =   "com.softwaremill.macwire"    %%    "runtime"           % "1.0.1"

  val scalaLogging    =   "com.typesafe.scala-logging"  %%    "scala-logging"     % "3.1.0"
  val logback         =   "ch.qos.logback"              %     "logback-classic"   % "1.1.1"

  val scalactic       =   "org.scalactic"               %%    "scalactic"         % "2.2.1"

  val scalaReflect    =   "org.scala-lang"              %     "scala-reflect"     % "2.11.6"

  val ramlJavaParser  =   "org.raml"                    %     "raml-parser"       % "0.9-SNAPSHOT"


  // test scope
  val scalaTest       =   "org.scalatest"               %%    "scalatest"         % "2.2.4"    % "test"
  val scramlgenTestCode   =  "io.atomicbits"            %%    "scramlgen-testdef" % "0.1.0-SNAPSHOT"

  val scramlgenDeps = Seq (
    scalaReflect
  )

  val scramlgenTestDeps = Seq (
    scramlgenTestCode
  )

  val scramlgenParserDeps = Seq(
    ramlJavaParser
  )

  val mainDeps = Seq(
    macwireMacros,
    macwireRuntime,
    scalaLogging,
    scalactic,
    logback
  )

  val testDeps = Seq(
    scalaTest
  )

  val allDeps = mainDeps ++ testDeps

}
