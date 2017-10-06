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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.parser.{ SourceFile, SourceReader }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 12/04/17.
  */
object DslSourceExtractor {

  @volatile
  var cache: Map[(String, String), Set[SourceFile]] = Map.empty

  /**
    * Extract all source files from the DSL jar dependency.
    *
    * @param platform The platform.
    * @return A set of source files wrapped in a Try monad. On read exceptions, the Try will be a Failure.
    */
  def extract()(implicit platform: Platform): Set[SourceFile] = {
    val baseDir   = platform.dslBaseDir
    val extension = platform.classFileExtension
    cache.getOrElse((baseDir, extension), fetchFiles(baseDir, extension))
  }

  private def fetchFiles(baseDir: String, extension: String): Set[SourceFile] = {
    val files =
      DslSourceExtractor.synchronized { // opening the same jar file several times in parallel... it doesn't work well
        Try(SourceReader.readResources(baseDir, s".$extension")) match {
          case Success(theFiles) => theFiles
          case Failure(exception) =>
            sys.error(
              s"""
                 |Could not read the DSL source files from $baseDir with extension $extension
                 |The exception was:
                 |${exception.getClass.getName}
                 |${exception.getMessage}
             """.stripMargin
            )
            Set.empty[SourceFile]
        }
      }
    add(baseDir, extension, files)
    files
  }

  private def add(baseDir: String, extension: String, files: Set[SourceFile]): Unit = {
    val baseDirAndExtension = (baseDir, extension)
    DslSourceExtractor.synchronized {
      cache += baseDirAndExtension -> files
    }
  }

}
