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

package io.atomicbits.scraml.generator.util

/**
  * Created by peter on 22/08/15.
  */
object CleanNameUtil {

  def cleanClassNameFromFileName(fileName: String): String = {
    val withOutExtension = fileName.split('.').filter(_.nonEmpty).head
    cleanClassName(withOutExtension)
  }

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

  def cleanMethodName: String => String = cleanFieldName

  def cleanEnumName: String => String = cleanFieldName

  def cleanFieldName(dirtyName: String): String = {
    // we don't do capitalization on field names, we keep them as close to the original as possible!
    // we cannot begin with a number, so we prepend a '$' when the first character is a number
    prepend$IfStartsWithNumber(dropInvalidCharacters(dirtyName))
  }

  private def prepend$IfStartsWithNumber(name: String): String = {
    if ((0 to 9).map(number => name.startsWith(number.toString)).reduce(_ || _)) "$" + name
    else name
  }

  private def dropInvalidCharacters(name: String): String = name.replaceAll("[^A-Za-z0-9$_]", "")

  def camelCased(dirtyName: String): String = {
    val chars = dirtyName.toCharArray
    chars(0) = chars(0).toLower
    new String(chars)
  }

  def cleanPackageName(dirtyName: String): String = {
    cleanClassName(dirtyName).toLowerCase
  }

  def quoteString(text: String): String = s""""$text""""

}
