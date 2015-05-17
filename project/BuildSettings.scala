import sbt._
import sbt.Keys._


trait BuildSettings { this:Build => 

  val scalacBuildOptions = Seq("-unchecked", "-deprecation", "-feature", "-Xlint")

  def projectSettings(dependencies:Seq[ModuleID]) = {
    Seq(
      scalacOptions := scalacBuildOptions,
      libraryDependencies ++= dependencies
    )
  }

}
