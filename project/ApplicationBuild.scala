import sbt.Keys._
import sbt._


object ApplicationBuild extends Build
with BuildSettings
with Dependencies {

  val scramlgenParser = Project(
    id = "scraml-parser",
    base = file("modules/scraml-parser"),
    settings = projectSettings(dependencies = scramlgenParserDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlgen = Project(
    id = "scraml-generator",
    base = file("modules/scraml-generator"),
    settings = projectSettings(dependencies = scramlgenDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    // Important: The paradise compiler plugin must be included in the project that defines the macro!
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    incOptions := incOptions.value.withNameHashing(false) // See issue: https://github.com/sbt/sbt/issues/1593
    ) dependsOn scramlgenParser

  val scramlgenTest = Project(
    id = "scraml-test",
    base = file("modules/scraml-test"),
    settings = projectSettings(dependencies = scramlgenTestDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val scramlgenTestDef = Project(
    id = "scraml-testdef",
    base = file("modules/scraml-testdef"),
    settings = projectSettings(dependencies = scramlgenDeps ++ testDeps)
  ) settings (
    // Sonatype snapshot resolver is needed to fetch raml-java-parser 0.9-SNAPSHOT.
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    // Obviously, the paradise compiler plugin must be included in the project that uses the macro!
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    incOptions := incOptions.value.withNameHashing(false) // See issue: https://github.com/sbt/sbt/issues/1593
    ) dependsOn scramlgen

  val main = Project(
    id = "scraml-project",
    base = file("."),
    settings = projectSettings(dependencies = allDeps)
  ) settings(
    publish :=(),
    publishLocal :=()
    ) aggregate(scramlgenParser, scramlgen, scramlgenTest, scramlgenTestDef)

}
