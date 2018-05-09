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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 9/12/16.
  */
trait CanonicalName {

  def name: String

  def packagePath: List[String]

}

case class RealCanonicalName private[canonicaltypes] (name: String, packagePath: List[String] = List.empty) extends CanonicalName {

  val value: String = s"${packagePath.mkString(".")}.$name"

}

case class NoName private[canonicaltypes] (packagePath: List[String] = List.empty) extends CanonicalName {

  override def name: String = "NoName"

}

object CanonicalName {

  def create(name: String, packagePath: List[String] = List.empty): CanonicalName =
    new RealCanonicalName(cleanClassName(name), cleanPackage(packagePath))

  def noName(packagePath: List[String]): CanonicalName = NoName(cleanPackage(packagePath))

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
