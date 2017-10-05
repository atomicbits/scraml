/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.parser

import java.io._
import java.net.{ URI, URL }
import java.nio.file.{ FileSystems, Files, Path, Paths, FileSystem }
import java.nio.file.{ FileSystem => _, _ }
import java.util.Collections

import scala.util.Try
import scala.collection.JavaConversions._

/**
  * Created by peter on 12/04/17.
  */
object SourceReader {

  /**
    * Read the content of a given source.
    *
    * @param source      The source to read.
    * @param charsetName The charset the file content is encoded in.
    * @return A pair consisting of a file path and the file content. Todo: refactor and make it return a SourceFile (aka io.atomicbits.scraml.generator.typemodel, but put it in the parser code)
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

    val uris: List[Option[URI]] = List(resource, classLoaderResource, file, url)
    val uri: URI                = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $source"))

    val (thePath, fs): (Path, Option[FileSystem]) =
      uri.getScheme match {
        case "jar" =>
          val fileSystem: FileSystem =
            Try(FileSystems.newFileSystem(uri, Collections.emptyMap[String, Any]))
              .recover {
                case exc: FileSystemAlreadyExistsException => FileSystems.getFileSystem(uri)
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

    val uris: List[Option[URI]] = List(resource, classLoaderResource)
    val uri: URI                = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $path"))

    val (thePath, fs): (Path, Option[FileSystem]) =
      uri.getScheme match {
        case "jar" =>
          val fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap[String, Any])
          (fileSystem.getPath(path), Some(fileSystem))
        case _ =>
          (Paths.get(uri), None)
      }

    val walk             = Files.walk(thePath)
    val paths: Set[Path] = walk.iterator.toSet

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

    path.foldLeft(rootPath) {
      case (aggr, component) => aggr.resolve(component.getFileName.toString)
    }
  }

  def getInputStreamContent(inputStream: InputStream): Array[Byte] =
    Stream.continually(inputStream.read).takeWhile(_ != -1).map(_.toByte).toArray

  /**
    * Beware, Windows paths are represented as URL as follows: file:///C:/Users/someone
    * uri.normalize().getPath then gives /C:/Users/someone instead of C:/Users/someone
    * also Paths.get(uri) then assumes /C:/Users/someone
    * One would think the java.nio.file implementation does it right, but it doesn't.
    *
    * This hack fixes this.
    *
    * see: http://stackoverflow.com/questions/18520972/converting-java-file-url-to-file-path-platform-independent-including-u
    *
    * http://stackoverflow.com/questions/9834776/java-nio-file-path-issue
    *
    */
  private def cleanWindowsTripleSlashIssue(path: String): String = {
    val hasWindowsPrefix =
      path.split('/').filter(_.nonEmpty).headOption.collect {
        case first if first.endsWith(":") => true
      } getOrElse false
    if (hasWindowsPrefix && path.startsWith("/")) path.drop(1)
    else path
  }

}
