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

  val scramlDsl = Project(
    id = "scraml-dsl",
    base = file("modules/scraml-dsl"),
    settings = buildSettings(dependencies = scramlDslDeps ++ testDeps)
  )

  val scramlGenerator = Project(
    id = "scraml-generator",
    base = file("modules/scraml-generator"),
    settings = buildSettings(dependencies = scramlGeneratorDeps ++ testDeps)
  )  dependsOn(scramlDsl, scramlParser, scramlJsonSchemaParser)

  val main = Project(
    id = "scraml-project",
    base = file("."),
    settings = buildSettings(dependencies = allDeps)
  ) settings(
    publish :=(),
    publishLocal :=()
    ) aggregate(scramlParser, scramlJsonSchemaParser, scramlDsl, scramlGenerator)

}
