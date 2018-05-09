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
import sbt.Keys._

object BuildSettings {

  val Organization = "io.atomicbits"

  val snapshotSuffix = "-SNAPSHOT"

  val scala2_10 = "2.10.6"
  val scala2_11 = "2.11.11"
  val scala2_12 = "2.12.3"

  val ScalaVersion = scala2_12

  val defaultCrossScalaVersions = Seq(scala2_10, scala2_11, scala2_12)

  val scalacBuildOptions =
    Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      // "-Xfatal-warnings",
      // "-Xlint:-infer-any",
      // "-Ywarn-value-discard",
      "-encoding",
      "UTF-8"
      // "-target:jvm-1.8",
      // "-Ydelambdafy:method"
    )

  def projectSettings(extraDependencies: Seq[ModuleID]) = Seq(
    organization := Organization,
    isSnapshot := version.value.endsWith(snapshotSuffix),
    scalaVersion := ScalaVersion,
    crossScalaVersions := defaultCrossScalaVersions,
    scalacOptions := scalacBuildOptions,
    parallelExecution := false,
    // Sonatype snapshot resolver is needed to fetch rxhttpclient-scala_2.11:0.2.0-SNAPSHOT.
    // resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= extraDependencies
  )

  val publishingCredentials = (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password))).getOrElse(Seq())

  val publishSettings = Seq(
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := pomInfo,
    credentials ++= publishingCredentials
  )

  def projSettings(dependencies: Seq[ModuleID]) = {
    projectSettings(dependencies) ++ publishSettings
  }

  lazy val pomInfo = <url>https://github.com/atomicbits/scraml</url>
    <licenses>
      <license>
        <name>AGPL licencse</name>
        <url>http://www.gnu.org/licenses/agpl-3.0.en.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:atomicbits/scraml.git</url>
      <connection>scm:git:git@github.com:atomicbits/scraml.git</connection>
    </scm>
    <developers>
      <developer>
        <id>rigolepe</id>
        <name>Peter Rigole</name>
        <url>http://atomicbits.io</url>
      </developer>
    </developers>

}
