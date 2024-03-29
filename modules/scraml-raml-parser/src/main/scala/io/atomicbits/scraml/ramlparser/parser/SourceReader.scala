/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.parser

import java.net.{ URI, URL }
import java.nio.file.{ FileSystems, Files, Path, Paths, FileSystem }
import java.nio.file.{ FileSystem => _, _ }
import java.util.Collections

import scala.util.Try
import scala.jdk.CollectionConverters._

/**
  * Created by peter on 12/04/17.
  */
object SourceReader {

  /**
    * Read the content of a given source.
    *
    * Mind that this function is NOT thread safe when reading from jar files.
    *
    * ToDo: extract & refactor the common parts in 'read' and 'readResources'
    *
    * @param source      The source to read.
    * @param charsetName The charset the file content is encoded in.
    * @return A pair consisting of a file path and the file content.
    */
  def read(source: String, charsetName: String = "UTF-8"): SourceFile = {

    // Resource loading
    // See: http://stackoverflow.com/questions/6608795/what-is-the-difference-between-class-getresource-and-classloader-getresource
    // What we see is that if resources start with a '/', then they get found by 'this.getClass.getResource'. If they don't start with
    // a '/', then they are found with 'this.getClass.getClassLoader.getResource'.
    val resource = Try(this.getClass.getResource(source).toURI).toOption
    // printReadStatus("regular resource", resource)
    // We want to assume that all resources given have an absolute path, also when they don't start with a '/'
    val classLoaderResource = Try(this.getClass.getClassLoader.getResource(source).toURI).toOption
    // printReadStatus("class loader resource", classLoaderResource)
    // File loading
    val file = Try(Paths.get(source)).filter(Files.exists(_)).map(_.toUri).toOption
    // printReadStatus("file resource", file)
    // URL loading
    val url = Try(new URL(source).toURI).toOption
    // printReadStatus("url resource", url)

    val uris: List[Option[URI]] = List(resource, classLoaderResource, file, url) // ToDo: replace with orElse structure & test
    val uri: URI                = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $source"))

    val (thePath, fs): (Path, Option[FileSystem]) =
      uri.getScheme match {
        case "jar" =>
          val fileSystem: FileSystem =
            Try(FileSystems.newFileSystem(uri, Collections.emptyMap[String, Any]))
              .recover {
                case exc: FileSystemAlreadyExistsException => FileSystems.getFileSystem(uri)
                // ToDo: see if reusing an open filesystem is a problem because it is closed below by the first thread that reaches that statement
              }
              .getOrElse {
                sys.error(s"Could not create or open filesystem for resource $uri")
              }
          (fileSystem.getPath(source), Some(fileSystem))
        case _ => (Paths.get(uri), None)
      }

    val encoded: Array[Byte] = Files.readAllBytes(thePath)

    fs.foreach(_.close())

    SourceFile(toDefaultFileSystem(thePath), new String(encoded, charsetName)) // ToDo: encoding detection via the file's BOM
  }

  /**
    * Read all files with a given extension in a given path, recursively through all subdirectories.
    *
    * Mind that this function is NOT thread safe when reading from jar files.
    *
    * http://www.uofr.net/~greg/java/get-resource-listing.html
    * http://alvinalexander.com/source-code/scala/create-list-all-files-beneath-directory-scala
    * http://stackoverflow.com/questions/31406471/get-resource-file-from-dependency-in-sbt
    * http://alvinalexander.com/blog/post/java/read-text-file-from-jar-file
    *
    * @param path        The path that we want to read all files from recursively.
    * @param extension   The extension of the files that we want to read.
    * @param charsetName The charset the file contents are encoded in.
    * @return
    */
  def readResources(path: String, extension: String, charsetName: String = "UTF-8"): Set[SourceFile] = {

    val resource            = Try(this.getClass.getResource(path).toURI).toOption
    val classLoaderResource = Try(this.getClass.getClassLoader.getResource(path).toURI).toOption

    val uris: List[Option[URI]] = List(resource, classLoaderResource) // ToDo: replace with orElse structure & test
    val uri: URI                = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $path"))

    val (thePath, fs): (Path, Option[FileSystem]) =
      uri.getScheme match {
        case "jar" =>
          val fileSystem =
            Try(FileSystems.newFileSystem(uri, Collections.emptyMap[String, Any]))
              .recover {
                case exc: FileSystemAlreadyExistsException => FileSystems.getFileSystem(uri)
                // ToDo: see if reusing an open filesystem is a problem because it is closed below by the first thread that reaches that statement
              }
              .getOrElse {
                sys.error(s"Could not create or open filesystem for resource $uri")
              }
          (fileSystem.getPath(path), Some(fileSystem))
        case _ =>
          (Paths.get(uri), None)
      }

    val walk             = Files.walk(thePath)
    val paths: Set[Path] = walk.iterator.asScala.toSet

    def isFileWithExtension(somePath: Path): Boolean =
      Files.isRegularFile(somePath) && somePath.getFileName.toString.toLowerCase.endsWith(extension.toLowerCase)

    val sourceFiles =
      paths.collect {
        case currentPath if isFileWithExtension(currentPath) =>
          val enc: Array[Byte] = Files.readAllBytes(currentPath)
          SourceFile(toDefaultFileSystem(currentPath), new String(enc, charsetName))
      }
    fs.foreach(_.close())
    sourceFiles
  }

  /**
    * Later on, we want to combine Path objects, but that only works when their filesystems are compatible,
    * so we convert paths that come out of a jar archive to the default filesystem.
    *
    * See: http://stackoverflow.com/questions/22611919/why-do-i-get-providermismatchexception-when-i-try-to-relativize-a-path-agains
    *
    * @param path The path to convert to the default filesystem.
    * @return The converted path.
    */
  def toDefaultFileSystem(path: Path): Path = {
    val rootPath =
      if (path.isAbsolute) Paths.get(FileSystems.getDefault.getSeparator)
      else Paths.get("")

    path.iterator().asScala.foldLeft(rootPath) {
      case (aggr, component) => aggr.resolve(component.getFileName.toString)
    }
  }
}
