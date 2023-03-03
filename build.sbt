import BuildSettings._
import Dependencies._

lazy val scramlDslScala = Project(
  id   = "scraml-dsl-scala",
  base = file("modules/scraml-dsl-scala")
).settings (
  projSettings(dependencies = scramlDslDepsScala ++ testDeps) ++
    Seq(
      // Copy all source files into the artifact.
      (Compile / unmanagedResourceDirectories) += (Compile / scalaSource).value
    )
)

lazy val scramlDslJava = Project(
  id   = "scraml-dsl-java",
  base = file("modules/scraml-dsl-java")
  // This is a pure Java project without scala versioning,
  // see http://stackoverflow.com/questions/8296280/use-sbt-to-build-pure-java-project
  // We also override the crossScalaVersions to avoid publish overwrite problems during release publishing, and because that
  // doesn't work (although I think it should), we also override the publishArtifact property.
).settings(
  projSettings(dependencies = scramlDslDepsJava ++ testDeps) ++
    Seq(
      crossPaths := false,
      crossScalaVersions := List(BuildSettings.scala2_12),
      autoScalaLibrary := false,
      // Copy all source files into the artifact.
      (Compile / unmanagedResourceDirectories) += (Compile / javaSource).value
    )
)

lazy val scramlDslAndroid = Project(
  id   = "scraml-dsl-android",
  base = file("modules/scraml-dsl-android")
  // This is a pure Java project without scala versioning,
  // see http://stackoverflow.com/questions/8296280/use-sbt-to-build-pure-java-project
  // We also override the crossScalaVersions to avoid publish overwrite problems during release publishing, and because that
  // doesn't work (although I think it should), we also override the publishArtifact property.
).settings(
  projSettings(dependencies = scramlDslDepsAndroid ++ testDeps) ++
    Seq(
      crossPaths := false,
      crossScalaVersions := List(BuildSettings.scala2_12),
      autoScalaLibrary := false,
      javacOptions ++= Seq("-source", "1.7"), // Android 4 till android 7 and above are on Java 1.7
      // Copy all source files into the artifact.
      (Compile / unmanagedResourceDirectories) += (Compile / javaSource).value
    )
)

lazy val scramlGenSimulation = Project(
  id       = "scraml-gen-simulation",
  base     = file("modules/scraml-gen-simulation")
).settings(
  projSettings(dependencies = scramlGeneratorDeps ++ testDeps)
) dependsOn (scramlDslScala, scramlDslJava)

lazy val scramlRamlParser = Project(
  id       = "scraml-raml-parser",
  base     = file("modules/scraml-raml-parser")
).settings(
  projSettings(dependencies = scramlRamlParserDeps ++ testDeps)
)

lazy val scramlGenerator = Project(
  id       = "scraml-generator",
  base     = file("modules/scraml-generator")
).settings(
  projSettings(dependencies = scramlGeneratorDeps ++ testDeps)
) dependsOn (scramlRamlParser, scramlDslScala, scramlDslJava, scramlDslAndroid)

lazy val main = Project(
  id       = "scraml-project",
  base     = file(".")
).settings(
    projSettings(dependencies = allDeps),
    crossScalaVersions := Nil,
    publish := ((): Unit),
    publishLocal := ((): Unit)
  ) aggregate (scramlRamlParser, scramlDslScala, scramlDslJava, scramlDslAndroid, scramlGenSimulation, scramlGenerator)
