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

package io.atomicbits.scraml.generator.formatting

import scalariform.formatter.preferences._
import scala.util.Try

// import org.scalafmt.{ Formatted, Scalafmt }

/**
  * Created by peter on 17/07/17.
  */
object ScalaFormatter {

  private val formatSettings =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(IndentSpaces, 2)

  def format(code: String): String = Try(scalariform.formatter.ScalaFormatter.format(code, formatSettings)).getOrElse(code)

  //  def format(code: String): String =
  //    Scalafmt.format(code) match {
  //      case Formatted.Success(formattedCode) => formattedCode
  //      case Formatted.Failure(e) =>
  //        println(s"WARNING: Could not format scala file: ${e.getMessage}")
  //        code
  //    }

}
