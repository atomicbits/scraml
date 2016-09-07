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
import sbtdoge.CrossPerProjectPlugin

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

  // builds the dsl but against play25
  val scramlDslPlay25Scala = {
    //if I choose just the module dir, the test sources get build in the main compile too :/
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/scraml-dsl-scala" / "src" /  "main" / "scala"))

    Project(
      id = "scraml-dsl-play25-scala",
      base = file("modules/scraml-dsl-play25-scala"),
      settings = buildSettings(dependencies = scramlDslPlay25DepsScala ++ testDeps)
    ).settings( unmanagedSourceDirectories in Compile <<= sourceDir )
      // play25 is enkel scala 2.11
     .settings(crossScalaVersions := Seq(scala2_11))
  }

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


  val scramlGenSimulation = Project(
    id = "scraml-gen-simulation",
    base = file("modules/scraml-gen-simulation"),
    settings = buildSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) dependsOn(scramlDslScala, scramlDslJava)


  val scramlRamlParser = Project(
    id = "scraml-raml-parser",
    base = file("modules/scraml-raml-parser"),
    settings = buildSettings(dependencies = scramlRamlParserDeps ++ testDeps)
  )


  val scramlGenerator = Project(
    id = "scraml-generator",
    base = file("modules/scraml-generator"),
    settings = buildSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  ) dependsOn(scramlJsonSchemaParser, scramlRamlParser)


  val main = Project(
    id = "scraml-project",
    base = file("."),
    settings = buildSettings(dependencies = allDeps)
  ).enablePlugins(CrossPerProjectPlugin)
   .settings(
    publish :=(),
    publishLocal :=()
    ) aggregate(scramlParser, scramlRamlParser, scramlJsonSchemaParser,
    scramlDslScala, scramlDslPlay25Scala, scramlDslJava, scramlGenSimulation, scramlGenerator
    )

}
