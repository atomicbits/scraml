/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.codegen

import java.nio.file.{ FileSystems, Path, Paths }
import java.util.regex.Pattern

import io.atomicbits.scraml.generator.platform.Platform
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
    * @param platform
    * @return
    */
  def rewrite(dslSource: SourceFile)(implicit platform: Platform): SourceFile = {
    val fromPackage: String          = platform.dslBasePackage
    val toPackageParts: List[String] = platform.rewrittenDslBasePackage
    val toPackage: String            = toPackageParts.mkString(".")
    val rewritten: String            = dslSource.content.replaceAll(Pattern.quote(fromPackage), toPackage)

    /**
      * Paths.get("", ...) makes a relative path under Linux/Mac (starts without slash) and Windows (starts with a single backslash '\')
      *
      * makeAbsolute(paths.get("", ...)) makes the path absolute under Linux/Mac (starts with a slash) and on Windows it remains with a
      *   single slash
      *
      * Paths.get(FileSystems.getDefault.getSeparator, ...) makes an absolute path under Linux/Mac (start with a slash), and makes an
      *   UNC path under Windows (start with two backslashes '\\'). Mind that a Windows path that starts with a single backslash is not
      *   compatible with a path that starts with double backslashes. The function 'relativize' cannot be applied to incompatible
      *   path types.
      *
      * ! ALWAYS TEST SCRAML THOUROUGHLY ON LINUX/MAC/WINDOWS WHEN CHANGING FILESYSTEM RELATED CODE LIKE THIS !
      */
    val dslBasePath: Path = makeAbsoluteOnLinuxMacKeepRelativeOnWindows(Paths.get("", platform.dslBasePackageParts: _*))

    // dslSource.filePath is an absolute path on Linux/Mac, a directory relative path on Windows
    val relativeFilePath: Path = dslBasePath.relativize(dslSource.filePath)
    val toPath: Path           = Paths.get(toPackageParts.head, toPackageParts.tail: _*)
    val newFilePath: Path      = toPath.resolve(relativeFilePath)
    dslSource.copy(filePath = newFilePath, content = rewritten)
  }

  def makeAbsoluteOnLinuxMacKeepRelativeOnWindows(path: Path): Path = {
    if (path.isAbsolute) path
    else {
      // Beware! The code below will make an absolute path from a relative path on Linux/Mac. It will keep a directory relative path
      // on windows as a directory relative path (that starts with a single backslash '\'). It may be confusing.
      Paths.get(FileSystems.getDefault.getSeparator).resolve(path)
    }
  }

}
