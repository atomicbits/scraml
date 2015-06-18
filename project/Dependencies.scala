
/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

import sbt._

trait Dependencies { this:Build =>

  // main dependencies
  val scalaLogging    =   "com.typesafe.scala-logging"  %%    "scala-logging"       % "3.1.0"
  val logback         =   "ch.qos.logback"              %     "logback-classic"     % "1.1.1"

  val rxHttpClient    =   "be.wegenenverkeer"           %%    "rxhttpclient-scala"  % "0.1.0-SNAPSHOT"
  val playJson        =   "com.typesafe.play"           %%    "play-json"           % "2.4.0"

  val scalaReflect    =   "org.scala-lang"              %     "scala-reflect"       % "2.11.6"

  val ramlJavaParser  =   "org.raml"                    %     "raml-parser"         % "0.9-SNAPSHOT"


  // test dependencies
  val scalaTest       =   "org.scalatest"               %%    "scalatest"           % "2.2.4"    % "test"
  val wiremock        =   "com.github.tomakehurst"      %     "wiremock"            % "1.56"     % "test"

  val scramlgenTestCode = "io.atomicbits"               %%    "scraml-testdef"      % "0.1.0-SNAPSHOT"



  // inclusion of the above dependencies in the modules
  val scramlGeneratorDeps = Seq (
    scalaReflect,
    rxHttpClient,
    playJson,
    wiremock
  )

  val scramlParserDeps = Seq(
    ramlJavaParser
  )

  val scramlJsonSchemaParserDeps = Seq(
    scalaLogging,
    logback,
    playJson
  )

  val mainDeps = Seq(
    scalaLogging,
    logback
  )

  val testDeps = Seq(
    scalaTest,
    wiremock
  )

  val allDeps = mainDeps ++ testDeps

}
