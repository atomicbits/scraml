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

package io.atomicbits.util

/**
  * Created by peter on 31/08/16.
  */
object TestUtils {

  /**
    * Pretty prints a textual representation of case classes.
    */
  def prettyPrint(obj: Object): String = {
    val text = Text(obj.toString)
    text.print()
  }

  def prettyPrint(text: String): String = Text(text).print()

  trait Text {

    val indentIncrement = 4

    def print(): String = print(0)

    def print(indent: Int): String

  }

  object Text {

    def apply(text: String): Text = {

      val cleanText = text.replaceAll(" ", "")

      def splitListElements(elements: String, splitChar: Char): List[String] = {

        val aggregate = (0, "", List.empty[String])

        val (level, last, elem) =
          elements.foldLeft(aggregate) {
            case ((0, current, elems), `splitChar`) => (0, "", elems :+ current)
            case ((x, current, elems), '(')         => (x + 1, s"${current}(", elems)
            case ((x, current, elems), ')')         => (x - 1, s"${current})", elems)
            case ((x, current, elems), char)        => (x, s"${current}$char", elems)
          }

        elem :+ last
      }

      val beginIndex = text.indexOf('(')
      val endIndex   = text.lastIndexOf(')')

      (beginIndex, endIndex) match {
        case (-1, -1) => SimpleText(cleanText)
        case (-1, _)  => sys.error(s"Invalid case class representation: $text")
        case (_, -1)  => sys.error(s"Invalid case class representation: $text")
        case (begin, end) =>
          val prefix        = text.take(begin)
          val contentString = text.drop(begin + 1).dropRight(text.length - end)

          if ("Map" == prefix) {
            val contentStrings = splitListElements(contentString, ',')
            val content = contentStrings.filter(_.nonEmpty).map { contentString =>
              val replaced = contentString.replaceFirst("->", "@")

              val index = replaced.indexOf('@')
              val key   = replaced.take(index)
              val text  = replaced.drop(index + 1)
              key -> Text(text)
            }
            MapBlock(content.toMap)
          } else {
            val contentStrings = splitListElements(contentString, ',')
            val content        = contentStrings.map(Text(_))
            Block(prefix, content)
          }
      }

    }

  }

  case class SimpleText(content: String) extends Text {

    def print(indent: Int): String = {

      val indentString = " " * indent

      s"$indentString$content"
    }

  }

  case class Block(prefix: String, content: List[Text]) extends Text {

    def print(indent: Int): String = {

      val indentString = " " * indent

      if (content.isEmpty) {
        s"$indentString$prefix()"
      } else {
        s"""$indentString$prefix(
           |${content.map(_.print(indent + indentIncrement)).mkString(",\n")}
           |$indentString)""".stripMargin
      }
    }

  }

  case class MapBlock(content: Map[String, Text]) extends Text {

    def print(indent: Int): String = {

      val indentString        = " " * indent
      val contentIndentString = " " * (indent + 1)

      val contentString = content.map {
        case (key, text) =>
          s"""$contentIndentString$key ->
             |${text.print(indent + 1 + indentIncrement)}""".stripMargin
      } mkString (",\n")

      if (content.isEmpty) {
        s"${indentString}Map()"
      } else {
        s"""${indentString}Map(
           |$contentString
           |$indentString)""".stripMargin
      }
    }

  }

}
