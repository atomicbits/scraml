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

import sbt.Keys._
import sbt._


object ApplicationBuild extends Build
with BuildSettings
with Dependencies {

  val scramlParser = Project(
    id = "scraml-parser",
    base = file("modules/scraml-parser"),
    settings = projectSettings(dependencies = scramlParserDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlJsonSchemaParser = Project(
    id = "scraml-jsonschema-parser",
    base = file("modules/scraml-jsonschema-parser"),
    settings = projectSettings(dependencies = scramlJsonSchemaParserDeps ++ testDeps)
  ) settings ()

  val scramlGenerator = Project(
    id = "scraml-generator",
    base = file("modules/scraml-generator"),
    settings = projectSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) settings(
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    // Important: The paradise compiler plugin must be included in the project that defines the macro!
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    incOptions := incOptions.value.withNameHashing(false) // See issue: https://github.com/sbt/sbt/issues/1593
    ) dependsOn (scramlParser, scramlJsonSchemaParser)

  val scramlTest = Project(
    id = "scraml-test",
    base = file("modules/scraml-test"),
    settings = projectSettings(dependencies = scramlTestDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlTestDef = Project(
    id = "scraml-testdef",
    base = file("modules/scraml-testdef"),
    settings = projectSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) settings(
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    // Obviously, the paradise compiler plugin must be included in the project that uses the macro!
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    incOptions := incOptions.value.withNameHashing(false), // See issue: https://github.com/sbt/sbt/issues/1593
    // add resources of the current project into the build classpath, see: http://stackoverflow.com/questions/17134244/reading-resources-from-a-macro-in-an-sbt-project
    unmanagedClasspath in Compile <++= unmanagedResources in Compile
    ) dependsOn scramlGenerator

  val main = Project(
    id = "scraml-project",
    base = file("."),
    settings = projectSettings(dependencies = allDeps)
  ) settings(
    publish :=(),
    publishLocal :=()
    ) aggregate(scramlParser, scramlJsonSchemaParser, scramlGenerator, scramlTest, scramlTestDef)

}
