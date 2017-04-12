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
import java.nio.file.{ Files, Paths }

import scala.util.Try

/**
  * Created by peter on 12/04/17.
  */
object SourceReader {

  def read(source: String, charsetName: String = "UTF-8"): (FilePath, String) = {

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
    def cleanWindowsTripleSlashIssue(path: String): String = {
      val hasWindowsPrefix =
        path.split('/').filter(_.nonEmpty).headOption.collect {
          case first if first.endsWith(":") => true
        } getOrElse false
      if (hasWindowsPrefix && path.startsWith("/")) path.drop(1)
      else path
    }

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

    val uri: URI = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $source"))

    val (path, encoded) =
      uri.getScheme match {
        case "jar" =>
          val filePath         = uri.normalize().toString.split('!').drop(1).head
          val pth              = filePath.split("/").dropRight(1).mkString("/")
          val inputStream      = this.getClass.getResourceAsStream(source) // Files.readAllBytes(Paths.get(uri)) doesn't work here.
          val enc: Array[Byte] = getInputStreamContent(inputStream)
          (pth, enc)
        case _ =>
          val pth              = uri.normalize().getPath.split("/").dropRight(1).mkString("/")
          val enc: Array[Byte] = Files.readAllBytes(Paths.get(uri))
          (pth, enc)
      }

    (FilePath(cleanWindowsTripleSlashIssue(path)), new String(encoded, charsetName)) // ToDo: encoding detection via the file's BOM
  }

  def getInputStreamContent(inputStream: InputStream): Array[Byte] =
    Stream.continually(inputStream.read).takeWhile(_ != -1).map(_.toByte).toArray

}
