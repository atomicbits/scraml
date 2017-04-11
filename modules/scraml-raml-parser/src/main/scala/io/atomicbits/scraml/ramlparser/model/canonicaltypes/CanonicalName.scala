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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

import io.atomicbits.scraml.ramlparser.model.AbsoluteId

/**
  * Created by peter on 9/12/16.
  */
case class CanonicalName private (name: String, packagePath: List[String] = List.empty) {

  val value: String = s"${packagePath.mkString(".")}.$name"

}

object CanonicalName {

  def create(name: String, packagePath: List[String] = List.empty): CanonicalName =
    new CanonicalName(cleanClassName(name), cleanPackage(packagePath))

  def cleanClassName(dirtyName: String): String = {
    // capitalize after special characters and drop those characters along the way
    // todo: instead of filtering out by listing the 'bad' characters, make a filter that is based on the 'good' characters.
    val capitalizedAfterDropChars =
      List('-', '_', '+', ' ', '/', '.', '~').foldLeft(dirtyName) { (cleaned, dropChar) =>
        cleaned.split(dropChar).filter(_.nonEmpty).map(_.capitalize).mkString("")
      }
    // capitalize after numbers 0 to 9, but keep the numbers
    val capitalized =
      (0 to 9).map(_.toString.head).toList.foldLeft(capitalizedAfterDropChars) { (cleaned, numberChar) =>
        // Make sure we don't drop the occurrences of numberChar at the end by adding a space and removing it later.
        val cleanedWorker = s"$cleaned "
        cleanedWorker.split(numberChar).map(_.capitalize).mkString(numberChar.toString).stripSuffix(" ")
      }
    // final cleanup of all strange characters
    prepend$IfStartsWithNumber(dropInvalidCharacters(capitalized))
  }

  def cleanPackage(pack: List[String]): List[String] = pack.map(cleanClassName).map(_.toLowerCase)

  def cleanClassNameFromFileName(fileName: String): String = {
    val withOutExtension = fileName.split('.').filter(_.nonEmpty).head
    cleanClassName(withOutExtension)
  }

  private def prepend$IfStartsWithNumber(name: String): String = {
    if ((0 to 9).map(number => name.startsWith(number.toString)).reduce(_ || _)) "$" + name
    else name
  }

  private def dropInvalidCharacters(name: String): String = name.replaceAll("[^A-Za-z0-9$_]", "")

}
