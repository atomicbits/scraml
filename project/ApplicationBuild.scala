import sbt.Keys._
import sbt._


object ApplicationBuild extends Build
with BuildSettings
with Dependencies {

  val scramlgenParser = Project(
    id = "scramlgen-parser",
    base = file("modules/scramlgen-parser"),
    settings = projectSettings(dependencies = scramlgenParserDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlgen = Project(
    id = "scramlgen",
    base = file("modules/scramlgen"),
    settings = projectSettings(dependencies = scramlgenDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    ) dependsOn scramlgenParser

  val scramlgenTest = Project(
    id = "scramlgen-test",
    base = file("modules/scramlgen-test"),
    settings = projectSettings(dependencies = scramlgenTestDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlgenTestDef = Project(
    id = "scramlgen-testdef",
    base = file("modules/scramlgen-testdef"),
    settings = projectSettings(dependencies = scramlgenDeps ++ testDeps)
  ) settings(
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    incOptions := incOptions.value.withNameHashing(false) // See issue: https://github.com/sbt/sbt/issues/1593
    ) dependsOn scramlgen

  val main = Project(
    id = "scramlgen-project",
    base = file("."),
    settings = projectSettings(dependencies = allDeps)
  ) settings(publish :=(), publishLocal :=()) aggregate(scramlgenParser, scramlgen, scramlgenTest, scramlgenTestDef)

}
