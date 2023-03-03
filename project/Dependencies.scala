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
  val logback = "ch.qos.logback" % "logback-classic" % "1.4.5"

  // java dsl dependencies
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.2"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core"        % "2.14.2"
  val jacksonDatabind    = "com.fasterxml.jackson.core" % "jackson-databind"    % "2.14.2"

  val snakeYaml = "org.yaml" % "snakeyaml" % "1.16"

  // val asyncClientOld      = "com.ning"             % "async-http-client" % "1.9.40" % "provided"
  val asyncClientProvided = "org.asynchttpclient"  % "async-http-client" % "2.12.3"  % "provided"
  val okHttpProvided      = "com.squareup.okhttp3" % "okhttp"            % "4.10.0"  % "provided"
  val playJson            = "com.typesafe.play"    %% "play-json"        % "2.9.4"

  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0"

  val scalariform  = "org.scalariform"                   %% "scalariform"       % "0.2.10"
  val javaFormat   = "com.google.googlejavaformat"       % "google-java-format" % "1.15.0"
  val mustacheJava = "com.github.spullara.mustache.java" % "compiler"           % "0.9.10"

  val slf4j = "org.slf4j" % "slf4j-api" % "2.0.5"

  // test dependencies
  val scalaTest       = "org.scalatest"          %% "scalatest"        % "3.2.15"  % "test"
  val wiremock        = "com.github.tomakehurst" % "wiremock"          % "2.27.2"  % "test"
  val junit           = "junit"                  % "junit"             % "4.13.2"  % "test"
  val asyncClientTest = "org.asynchttpclient"    % "async-http-client" % "2.12.3"  % "test"

  val scramlRamlParserDeps = Seq(
    playJson,
    snakeYaml,
    slf4j,
    scalaCollectionCompat
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
    asyncClientProvided,
    scalaCollectionCompat
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
