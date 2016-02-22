
val dslGen = taskKey[Seq[File]]("Copy DSL code to resources to include in later code generation steps.")

dslGen := {
  val resourceDir = (resourceDirectory in Compile).value
  val dst = new File(resourceDir, "dsl")
  IO.delete(dst)
  dst.mkdirs()
  val sourceBase = (sourceDirectory in Compile).value
  val sourceFiles = (unmanagedSources in Compile).value
  sourceFiles.foreach { file =>
    val path = file.relativeTo(sourceBase).get.getPath
    val relativeFile = new File(dst, path)
    relativeFile.getParentFile.mkdirs()
    IO.copyFile(file, relativeFile)
  }
  Seq.empty[File] // We don't actually need to compile the files, so we return an empty list.
}

sourceGenerators in Compile += (dslGen in Compile).taskValue
