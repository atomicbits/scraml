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
import sbt.Keys._

object BuildSettings {

  val Organization = "io.atomicbits"

  val snapshotSuffix = "-SNAPSHOT"

  val Version = "0.7.2" // + snapshotSuffix // Change in 1 place

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
    version := Version,
    isSnapshot := Version.endsWith(snapshotSuffix),
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
        None // Some("snapshots" at nexus + "content/repositories/snapshots")
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
