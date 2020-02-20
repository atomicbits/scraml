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
      }.mkString(",\n")

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
