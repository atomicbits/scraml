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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.parser.{ SourceFile, SourceReader }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 12/04/17.
  */
object DslSourceExtractor {

  val logger: Logger = LoggerFactory.getLogger(DslSourceExtractor.getClass)

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
            logger.debug(
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
