import BuildSettings._
import Dependencies._

lazy val scramlDslScala = Project(
  id   = "scraml-dsl-scala",
  base = file("modules/scraml-dsl-scala"),
  settings = projSettings(dependencies = scramlDslDepsScala ++ testDeps) ++
    Seq(
      // Copy all source files into the artifact.
      (unmanagedResourceDirectories in Compile) += (scalaSource in Compile).value
    )
)

// builds the dsl but against play25
lazy val scramlDslPlay25Scala = {
  //if I choose just the module dir, the test sources get build in the main compile too :/
  val sourceDir = (baseDirectory in ThisBuild)(b => Seq(b / "modules/scraml-dsl-scala" / "src" / "main" / "scala"))

  Project(
    id       = "scraml-dsl-play25-scala",
    base     = file("modules/scraml-dsl-play25-scala"),
    settings = projSettings(dependencies = scramlDslPlay25DepsScala ++ testDeps)
  ).settings(unmanagedSourceDirectories in Compile <<= sourceDir)
    // play25 is enkel scala 2.11
    .settings(crossScalaVersions := Seq(scala2_11))
}

lazy val scramlDslJava = Project(
  id   = "scraml-dsl-java",
  base = file("modules/scraml-dsl-java"),
  // This is a pure Java project without scala versioning,
  // see http://stackoverflow.com/questions/8296280/use-sbt-to-build-pure-java-project
  // We also override the crossScalaVersions to avoid publish overwrite problems during release publishing, and because that
  // doesn't work (although I think it should), we also override the publishArtifact property.
  settings = projSettings(dependencies = scramlDslDepsJava ++ testDeps) ++
    Seq(
      crossPaths := false,
      autoScalaLibrary := false,
      publishArtifact <<= scalaVersion { sv =>
        sv != BuildSettings.ScalaVersion
      }, // , crossScalaVersions := Seq(ScalaVersion)
      // Copy all source files into the artifact.
      (unmanagedResourceDirectories in Compile) += (javaSource in Compile).value
    )
)

lazy val scramlGenSimulation = Project(
  id       = "scraml-gen-simulation",
  base     = file("modules/scraml-gen-simulation"),
  settings = projSettings(dependencies = scramlGeneratorDeps ++ testDeps)
) dependsOn (scramlDslScala, scramlDslJava)

lazy val scramlRamlParser = Project(
  id       = "scraml-raml-parser",
  base     = file("modules/scraml-raml-parser"),
  settings = projSettings(dependencies = scramlRamlParserDeps ++ testDeps)
)

lazy val scramlGenerator = Project(
  id       = "scraml-generator",
  base     = file("modules/scraml-generator"),
  settings = projSettings(dependencies = scramlGeneratorDeps ++ testDeps)
) dependsOn (scramlRamlParser, scramlDslScala, scramlDslJava)

lazy val main = Project(
  id       = "scraml-project",
  base     = file("."),
  settings = projSettings(dependencies = allDeps)
).enablePlugins(CrossPerProjectPlugin)
  .settings(
    publish := (),
    publishLocal := ()
  ) aggregate (scramlRamlParser,
scramlDslScala, scramlDslPlay25Scala, scramlDslJava, scramlGenSimulation, scramlGenerator)
