
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
//  val scalaLogging    =   "com.typesafe.scala-logging"  %%    "scala-logging"       % "3.1.0"
  val logback         =   "ch.qos.logback"              %     "logback-classic"     % "1.1.1"

  val rxHttpClient    =   "be.wegenenverkeer"           %%    "rxhttpclient-scala"  % "0.2.0"
  val playJson        =   "com.typesafe.play"           %%    "play-json"           % "2.4.3"

  val ramlJavaParser  =   "org.raml"                    %     "raml-parser"         % "0.8.11"

  val scalariform     =   "org.scalariform"             %%    "scalariform"         % "0.1.7"

  // test dependencies
  val scalaTest       =   "org.scalatest"               %%    "scalatest"           % "2.2.4"    % "test"
  val wiremock        =   "com.github.tomakehurst"      %     "wiremock"            % "1.56"     % "test"



  // inclusion of the above dependencies in the modules
  val scramlGeneratorDeps = Seq (
    rxHttpClient,
    playJson,
    wiremock,
    scalariform
  )

  val scramlGeneratorTestDefDeps = Seq (
    rxHttpClient,
    playJson,
    wiremock
  )

  val scramlParserDeps = Seq(
    ramlJavaParser
  )

  val scramlJsonSchemaParserDeps = Seq(
    logback,
    playJson
  )

  val scramlDslDeps = Seq(
    playJson,
    rxHttpClient
  )

  val mainDeps = Seq(
    logback
  )

  val testDeps = Seq(
    scalaTest,
    wiremock
  )

  val allDeps = mainDeps ++ testDeps

}
