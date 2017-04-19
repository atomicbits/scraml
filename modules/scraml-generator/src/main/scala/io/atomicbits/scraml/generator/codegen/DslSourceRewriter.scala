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

import java.nio.file.{ FileSystem, FileSystems, Path, Paths }
import java.util.regex.Pattern

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.platform.javajackson.JavaJackson
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 18/04/17.
  */
object DslSourceRewriter {

  /**
    *
    * put the DSL support codebase under
    * basepackage.dsl.javajackson.*
    * basepackage.dsl.scalaplay.*
    *
    * @param dslSource
    * @param apiBasePackage
    * @param platform
    * @return
    */
  def rewrite(dslSource: SourceFile, apiBasePackage: List[String])(implicit platform: Platform): SourceFile = {
    val fromPackage: String          = platform.dslBasePackage
    val toPackageParts: List[String] = rewrittenDslBasePackage(apiBasePackage)
    val toPackage: String            = toPackageParts.mkString(".")
    val rewritten: String            = dslSource.content.replaceAll(Pattern.quote(fromPackage), toPackage)
    val dslBasePath: Path            = Paths.get(FileSystems.getDefault.getSeparator, platform.dslBasePackageParts: _*) // absolute path

    println(s"dslBasePath is: $dslBasePath")
    println(s"dslSource.filePath is: ${dslSource.filePath}")
    println(s"makeAbsolute(dslSource.filePath) is: ${makeAbsolute(dslSource.filePath)}")

    val relativeFilePath: Path = dslBasePath.relativize(makeAbsolute(dslSource.filePath)) // dslSource.filePath is an absolute path
    val toPath: Path           = Paths.get(toPackageParts.head, toPackageParts.tail: _*)
    val newFilePath: Path      = toPath.resolve(relativeFilePath)
    dslSource.copy(filePath = newFilePath, content = rewritten)
  }

  def makeAbsolute(path: Path): Path = {
    if (path.isAbsolute) path
    else Paths.get(FileSystems.getDefault.getSeparator).resolve(path)
  }

  def rewrittenDslBasePackage(apiBasePackage: List[String])(implicit platform: Platform): List[String] = {
    platform match {
      case JavaJackson => apiBasePackage ++ List("dsl", "javajackson")
      case ScalaPlay   => apiBasePackage ++ List("dsl", "scalaplay")
    }
  }

}
