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
    val capitalizedAfterDropChars =
      List('-', '_', '+', ' ', '/', '.').foldLeft(dirtyName) { (cleaned, dropChar) =>
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
    capitalized.replaceAll("[^A-Za-z0-9]", "")
  }


  def cleanMethodName(dirtyName: String): String = {
    // capitalize after special characters and drop those characters along the way
    List('-', '_', '+', ' ', '/', '.').foldLeft(dirtyName) { (cleaned, dropChar) =>
      cleaned.split(dropChar).filter(_.nonEmpty).map(_.capitalize).mkString("")
    }
  }


  def cleanFieldName(dirtyName: String): String = {
    // an underscore is allowed!
    // we don't do capitalization on field names, we keep them as close to the original as possible!
    val dropCharred =
      List('-', '+', ' ', '/', '.').foldLeft(dirtyName) { (cleaned, dropChar) =>
        cleaned.split(dropChar).filter(_.nonEmpty).mkString("")
      }
    // we cannot begin with a number
    dropCharred.toList.foldLeft(List.empty[Char]) { (collected, nextChar) =>
      if (collected.isEmpty && (0 to 9).map(_.toString.head).contains(nextChar)) collected
      else collected :+ nextChar
    } mkString ""
  }


  def camelCased(dirtyName: String): String = {
    val chars = dirtyName.toCharArray
    chars(0) = chars(0).toLower
    new String(chars)
  }


  def cleanPackageName(dirtyName: String): String = {
    cleanClassName(dirtyName).toLowerCase
  }


  def escapeJavaKeyword(someName: String, escape: String = "$"): String = {

    val javaReservedWords =
      List("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
        "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while")

    javaReservedWords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"$name$escape"
      else name
    }

  }


  def escapeScalaKeyword(someName: String, escape: String = "$"): String = {
    val scalaReservedwords =
      List("Byte", "Short", "Char", "Int", "Long", "Float", "Double", "Boolean", "Unit", "String", "abstract", "case", "catch", "class",
        "def", "do", "else", "extends", "false", "final", "finally", "for", "forSome", "if", "implicit", "import", "lazy", "match", "new",
        "null", "object", "override", "package", "private", "protected", "return", "sealed", "super", "this", "throw", "trait", "try",
        "true", "type", "val", "var", "while", "with", "yield")


    scalaReservedwords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"$name$$"
      else name
    }

  }

}
