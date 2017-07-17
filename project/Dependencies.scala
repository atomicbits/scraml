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

object Dependencies {

  // main dependencies
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.1"

  // java dsl dependencies
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % "2.5.4"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core"        % "2.5.4"
  val jacksonDatabind    = "com.fasterxml.jackson.core" % "jackson-databind"    % "2.5.4"

  val snakeYaml = "org.yaml" % "snakeyaml" % "1.16"

  val asyncClientProvided = "com.ning"          % "async-http-client" % "1.9.40" % "provided"
  val playJson            = "com.typesafe.play" %% "play-json"        % "2.6.2"

  val scalaFmt   = "com.geirsson"                %% "scalafmt-core"     % "1.1.0"
  val javaFormat = "com.google.googlejavaformat" % "google-java-format" % "1.2"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"

  // test dependencies
  val scalaTest       = "org.scalatest"          %% "scalatest"        % "3.0.3"  % "test"
  val wiremock        = "com.github.tomakehurst" % "wiremock"          % "1.57"   % "test"
  val junit           = "junit"                  % "junit"             % "4.12"   % "test"
  val asyncClientTest = "com.ning"               % "async-http-client" % "1.9.40" % "test"

  val scramlRamlParserDeps = Seq(
    playJson,
    snakeYaml,
    slf4j
  )

  // inclusion of the above dependencies in the modules
  val scramlGeneratorDeps = Seq(
    scalaFmt,
    javaFormat,
    junit
  )

  val scramlDslDepsScala = Seq(
    slf4j,
    playJson,
    asyncClientProvided
  )

//  val scramlDslPlay25DepsScala = Seq(
//    slf4j,
//    play25Json,
//    asyncClientProvided
//  )

  val scramlDslDepsJava = Seq(
    slf4j,
    junit,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    asyncClientProvided
  )

  val mainDeps = Seq(
    logback
  )

  val testDeps = Seq(
    scalaTest,
    wiremock,
    asyncClientTest
  )

  val allDeps = mainDeps ++ testDeps

}
