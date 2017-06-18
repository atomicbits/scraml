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

import scala.util.Try

/**
  * Created by peter on 12/04/17.
  */
object DslSourceExtractor {

  /**
    * Extract all source files from the DSL jar dependency.
    *
    * @param platform The platform.
    * @return A set of source files wrapped in a Try monad. On read exceptions, the Try will be a Failure.
    */
  def extract()(implicit platform: Platform): Try[Set[SourceFile]] =
    Try(SourceReader.readResources(platform.dslBaseDir, s".${platform.classFileExtension}"))

}
