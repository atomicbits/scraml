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
    settings = buildSettings(dependencies = scramlParserDeps ++ testDeps)
  )

  val scramlJsonSchemaParser = Project(
    id = "scraml-jsonschema-parser",
    base = file("modules/scraml-jsonschema-parser"),
    settings = buildSettings(dependencies = scramlJsonSchemaParserDeps ++ testDeps)
  )

  val scramlDslScala = Project(
    id = "scraml-dsl-scala",
    base = file("modules/scraml-dsl-scala"),
    settings = buildSettings(dependencies = scramlDslDepsScala ++ testDeps)
  )

  val scramlDslJava = Project(
    id = "scraml-dsl-java",
    base = file("modules/scraml-dsl-java"),
    // This is a pure Java project without scala versioning,
    // see http://stackoverflow.com/questions/8296280/use-sbt-to-build-pure-java-project
    // We also override the crossScalaVersions to avoid publish overwrite problems during release publishing, and because that
    // doesn't work (although I think it should), we also override the publishArtifact property.
    settings = buildSettings(dependencies = scramlDslDepsJava ++ testDeps) ++
      Seq(
        crossPaths := false,
        autoScalaLibrary := false,
        publishArtifact <<= scalaVersion { sv => sv != ScalaVersion }
        // , crossScalaVersions := Seq(ScalaVersion)
      )
  )


  val scramlGenerator = Project(
    id = "scraml-generator",
    base = file("modules/scraml-generator"),
    settings = buildSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) dependsOn(scramlParser, scramlJsonSchemaParser)


  val scramlGenSimulation = Project(
    id = "scraml-gen-simulation",
    base = file("modules/scraml-gen-simulation"),
    settings = buildSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) dependsOn(scramlDslScala, scramlDslJava)


  val scramlRamlParser = Project(
    id = "scraml-raml-parser",
    base = file("modules/scraml-raml-parser"),
    settings = buildSettings(dependencies = scramlRamlParserDeps ++ testDeps)
  ) dependsOn(scramlDslScala, scramlDslJava)


  val main = Project(
    id = "scraml-project",
    base = file("."),
    settings = buildSettings(dependencies = allDeps)
  ) settings(
    publish :=(),
    publishLocal :=()
    ) aggregate(scramlParser, scramlRamlParser, scramlJsonSchemaParser, scramlDslScala, scramlDslJava, scramlGenSimulation, scramlGenerator)

}
