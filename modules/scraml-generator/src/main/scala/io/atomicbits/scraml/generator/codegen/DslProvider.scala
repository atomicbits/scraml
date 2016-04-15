/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.codegen

import java.io.File
import java.net.URI
import java.nio.file._
import java.util

import com.sun.nio.zipfs.ZipPath
import io.atomicbits.scraml.generator.model._

import scala.annotation.tailrec
import scala.util.Try
import scala.collection.JavaConverters._
import scala.language.postfixOps

/**
  * Created by peter on 15/04/16.
  */
object DslProvider {

  def generateDslClasses(language: Language, packageBasePath: List[String]): Seq[ClassRep] = {

    read(language).collect {
      case (fullFileName, fileContent) =>
        val fileParts = fullFileName.split('/')
        val fileName = fileParts.takeRight(1).head
        val packageParts = fileParts.dropRight(1).toList
        CommonClassRep(ClassReference(fileName, packageParts)).withContent(fileContent)
    } toSeq

  }


  private def read(language: Language): Map[String, String] = {

    val (relativeDslBase, extension) =
      language match {
        case Scala => ("/dsl/scala", ".scala")
        case Java => ("/dsl/java", ".java")
      }
    println(s"Relative DSL base: $relativeDslBase")
    val dslBaseUriOpt = Try(this.getClass.getResource(relativeDslBase).toURI).toOption
    val dslBaseUrl: URI = dslBaseUriOpt.getOrElse(sys.error(s"Unable to find sources for $language DSL support classes."))
    println(s"DSL base URI: $dslBaseUrl")

    val (dlsBasePath, fileSystemOpt) =
      if (dslBaseUrl.toString.contains('!')) {
        // See: http://stackoverflow.com/questions/22605666/java-access-files-in-jar-causes-java-nio-file-filesystemnotfoundexception
        val dslBaseUrlArr = dslBaseUrl.toString.split("!")
        val fs: FileSystem = FileSystems.newFileSystem(URI.create(dslBaseUrlArr(0)), new util.HashMap[String, String])
        (fs.getPath(dslBaseUrlArr(1)), Some(fs))
      } else {
        (Paths.get(dslBaseUrl), None)
      }


    def recurseDirs(path: Path): Map[String, String] = {
      val maps =
        Files.list(path).iterator().asScala.collect {
          case fileOrDir if Files.isRegularFile(fileOrDir) =>
            val relativeFilePath = dlsBasePath.relativize(fileOrDir).toString.stripSuffix(extension)
            val encoded: Array[Byte] = Files.readAllBytes(fileOrDir)
            val fileContent = new String(encoded, "UTF-8")
            Map(relativeFilePath -> fileContent)
          case fileOrDir if Files.isDirectory(fileOrDir) =>
            recurseDirs(fileOrDir)
        }
      maps.foldLeft(Map.empty[String, String]) { (aggr, map) => aggr ++ map }
    }

    val files = recurseDirs(dlsBasePath)
    fileSystemOpt.foreach(_.close())
    files
  }

}
