/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
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

  val asyncClientProvided = "com.ning"             % "async-http-client" % "1.9.40" % "provided"
  val okHttpProvided      = "com.squareup.okhttp3" % "okhttp"            % "3.9.0" % "provided"
  val playJson            = "com.typesafe.play"    %% "play-json"        % "2.8.1"

  val scalariform  = "org.scalariform"                   %% "scalariform"       % "0.2.10"
  val javaFormat   = "com.google.googlejavaformat"       % "google-java-format" % "1.2"
  val mustacheJava = "com.github.spullara.mustache.java" % "compiler"           % "0.9.5"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"

  // test dependencies
  val scalaTest       = "org.scalatest"          %% "scalatest"        % "3.1.0"  % "test"
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
    scalariform,
    javaFormat,
    mustacheJava,
    junit
  )

  val scramlDslDepsScala = Seq(
    slf4j,
    playJson,
    asyncClientProvided
  )

  val scramlDslDepsAndroid = Seq(
    slf4j,
    junit,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    okHttpProvided
  )

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
