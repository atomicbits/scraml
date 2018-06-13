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

package io.atomicbits.scraml.ramlparser.model

import java.util.Locale

import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json._
import io.atomicbits.scraml.util.TryUtils._

import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.{ Failure, Try }

/**
  * Created by peter on 25/05/17.
  */
trait ModelMerge {

  val replaceStringPattern: Regex = """<<([^<>]*)>>""".r

  def findMergeNames(jsObject: JsObject, selectionKey: String): MergeApplicationMap = {
    (jsObject \ selectionKey).toOption
      .collect {
        case JsString(value)      => Json.obj() + (value -> Json.obj())
        case typesJsObj: JsObject => typesJsObj
        case typesJsArr: JsArray  => KeyedList.toJsObject(typesJsArr)
      }
      .map(MergeApplicationMap(_))
      .getOrElse(MergeApplicationMap())
  }

  def applyToForMergeNames(jsObject: JsObject,
                           mergeApplicationMap: MergeApplicationMap,
                           mergeDeclaration: Map[String, JsObject],
                           optionalTopLevelField: Boolean = false)(implicit parseContext: ParseContext): Try[JsObject] = {

    val toMerge: Try[Seq[JsObject]] =
      accumulate(
        mergeApplicationMap.map {
          case (mergeName, mergeApplication) =>
            Try(mergeDeclaration(mergeName))
              .recoverWith {
                case e => Failure(RamlParseException(s"Unknown trait or resourceType name $mergeName in ${parseContext.head}."))
              }
              .map { jsObj =>
                applyMerge(jsObj, mergeApplication)
              }
        } toSeq
      )

    val deepMerged =
      toMerge.map { mergeBlocks =>
        mergeBlocks.foldLeft(jsObject) { (aggr, currentMergeBlock) =>
          deepMerge(currentMergeBlock, aggr, optionalTopLevelField)
        }
      }

    deepMerged
  }

  /**
    * Deep merge of the source json object into the target json object according to the
    * rules defined by
    * https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/#algorithm-of-merging-traits-and-methods
    *
    * Summarized: The target object's fields are NOT overwritten by the source fields. Object fields are recursively merged
    * and array fields are merged by value.
    *
    * 1. Method node properties are inspected and those that are undefined in trait node remain unchanged.
    * 2. The method node receives all properties of trait node (excluding optional ones), which are undefined in the method node.
    * 3. Properties defined in both method node and trait node (including optional ones) are treated as follows:
    *    - Scalar properties remain unchanged.
    *    - Collection properties are merged by value, as described later.
    *    - Values of object properties are subjected to steps 1-3 of this procedure.
    *
    */
  protected def deepMerge(source: JsObject, target: JsObject, optionalTopLevelField: Boolean = false): JsObject = {

    /**
      * Deep merges the fieldWithValue into the aggr json object.
      */
    def mergeFieldInto(aggregatedTarget: JsObject, fieldWithValue: (String, JsValue)): JsObject = {

      val (field, value) = fieldWithValue

      val (wasOptionalField, actualField) =
        if (optionalTopLevelField && field.endsWith("?")) (true, field.dropRight(1))
        else (false, field)

      (aggregatedTarget \ actualField).toOption match {
        case Some(aggrValue: JsObject) =>
          value match {
            case jsOb: JsObject => aggregatedTarget + (actualField -> deepMerge(jsOb, aggrValue))
            case _              => aggregatedTarget
          }
        case Some(aggrValue: JsArray) =>
          value match {
            case jsArr: JsArray => aggregatedTarget + (actualField -> mergeArrays(jsArr, aggrValue))
            case _              => aggregatedTarget
          }
        case Some(aggrValue)          => aggregatedTarget
        case None if wasOptionalField => aggregatedTarget
        case None                     => aggregatedTarget + (actualField -> value)
      }

    }

    /**
      * Merges the source array into the target array by value.
      */
    def mergeArrays(jsArrSource: JsArray, jsArrTarget: JsArray): JsArray = {
      jsArrSource.value.foldLeft(jsArrTarget) { (aggr, sourceValue) =>
        if (!aggr.value.contains(sourceValue)) aggr.+:(sourceValue)
        else aggr
      }
    }

    source.value.toMap.foldLeft(target)(mergeFieldInto)
  }

  private[model] def fetchReplaceStrings(text: String): Seq[ReplaceString] = {
    replaceStringPattern
      .findAllIn(text)
      .matchData
      .map { m =>
        ReplaceString(m.matched, m.group(1), m.matched != text)
      }
      .toSeq
  }

  private def applyMerge(jsObject: JsObject, mergeApplication: MergeApplication)(implicit parseContext: ParseContext): JsObject = {

    def mergeStringAsJsValue(text: String): JsValue = {
      val replaceStrings: Seq[ReplaceString] = fetchReplaceStrings(text)
      if (replaceStrings.size == 1 && !replaceStrings.head.partial) {
        mergeApplication.get(replaceStrings.head.matchString) match {
          case Some(jsValue) => jsValue
          case None          => sys.error(s"Did not find trait or resourceType replacement for value ${replaceStrings.head.matchString}.")
        }
      } else {
        val replaced = mergeStringAsStringHelper(text, replaceStrings)
        JsString(replaced)
      }
    }

    def mergeStringAsString(text: String): String = {
      val replaceStrings: Seq[ReplaceString] = fetchReplaceStrings(text)
      mergeStringAsStringHelper(text, replaceStrings)
    }

    def mergeStringAsStringHelper(text: String, replaceStrings: Seq[ReplaceString]): String = {

      replaceStrings.foldLeft(text) { (aggrText, replaceString) =>
        val replacement: String =
          mergeApplication.get(replaceString.matchString) match {
            case Some(JsString(stringVal))   => stringVal
            case Some(JsBoolean(booleanVal)) => booleanVal.toString
            case Some(JsNumber(number))      => number.toString
            case Some(jsValue) =>
              sys.error(s"Cannot replace the following ${replaceString.matchString} value in a trait or resourceType: $jsValue")
            case None => sys.error(s"Did not find trait or resourceType replacement for value ${replaceString.matchString}.")
          }
        val transformedReplacement = replaceString.transformReplacementString(replacement)
        text.replaceAllLiterally(replaceString.toReplace, transformedReplacement)
      }

    }

    def mergeJsObject(jsObject: JsObject): JsObject = {
      val mergedFields =
        jsObject.fields.map {
          case (fieldName, jsValue) => (mergeStringAsString(fieldName), mergeJsValue(jsValue))
        }
      JsObject(mergedFields)
    }

    def mergeJsValue(jsValue: JsValue): JsValue =
      jsValue match {
        case jsObject: JsObject  => mergeJsObject(jsObject)
        case JsString(stringVal) => mergeStringAsJsValue(stringVal)
        case JsArray(items)      => JsArray(items.map(mergeJsValue))
        case other               => other
      }

    mergeJsObject(jsObject)
  }

}

/**
  * In:
  *
  * /books:
  *   type: { searchableCollection: { queryParamName: title, fallbackParamName: digest_all_fields } }
  *   get:
  *     is: [ secured: { tokenName: access_token }, paged: { maxPages: 10 } ]
  *
  * these are the MergeSubstitutions:
  *
  * MergeSubstitution(tokenName, access_token)
  * MergeSubstitution(maxPages, 10)
  *
  * and the MergeApplications:
  *
  * MergeApplication(secured, ...)
  * MergeApplication(paged, ...)
  *
  * @param name
  * @param value
  */
case class MergeSubstitution(name: String, value: JsValue)

case class MergeApplication(name: String, substitutions: Seq[MergeSubstitution]) {

  private val mergeDef: Map[String, JsValue] = substitutions.map(sub => (sub.name, sub.value)).toMap

  def get(name: String)(implicit parseContext: ParseContext): Option[JsValue] = {
    mergeDef
      .get(name)
      .orElse {
        name match {
          case "resourcePath"     => Some(JsString(parseContext.resourcePath))
          case "resourcePathName" => Some(JsString(parseContext.resourcePathName))
          case other              => None
        }
      }
  }

  def isEmpty: Boolean = substitutions.isEmpty

}

object MergeApplication {

  def apply(name: String, jsObj: JsObject): MergeApplication = {
    val substitutions =
      jsObj.value.map {
        case (nm, value) => MergeSubstitution(nm, value)
      } toSeq

    MergeApplication(name, substitutions)
  }

}

case class MergeApplicationMap(mergeApplications: Seq[MergeApplication] = Seq.empty) {

  val mergeMap: Map[String, MergeApplication] = mergeApplications.map(md => (md.name, md)).toMap

  def get(name: String): Option[MergeApplication] = mergeMap.get(name)

  def map[T](f: ((String, MergeApplication)) => T): Iterable[T] = mergeMap.map(f)

}

object MergeApplicationMap {

  def apply(jsObj: JsObject): MergeApplicationMap = {
    val mergeDefinitions =
      jsObj.value.collect {
        case (name, subst: JsObject) => MergeApplication(name, subst)
        case (name, _)               => MergeApplication(name, Json.obj())
      } toSeq

    MergeApplicationMap(mergeDefinitions)
  }

}

sealed trait ReplaceOp {

  def apply(text: String): String

}

object ReplaceOp {

  def apply(opString: String): ReplaceOp =
    opString match {
      case "!singularize"         => Singularize
      case "!pluralize"           => Pluralize
      case "!uppercase"           => Uppercase
      case "!lowercase"           => Lowercase
      case "!lowercamelcase"      => LowerCamelcase
      case "!uppercamelcase"      => UpperCamelcase
      case "!lowerunderscorecase" => LowerUnderscorecase
      case "!upperunderscorecase" => UpperUnderscorecase
      case "!lowerhyphencase"     => LowerHyphencase
      case "!upperhyphencase"     => UpperHyphencase
      case unknown                => NoOp
    }

}

case object Singularize extends ReplaceOp {

  def apply(text: String): String = {
    if (text.endsWith("s")) text.dropRight(1)
    else text
  }

}

case object Pluralize extends ReplaceOp {

  def apply(text: String): String = {
    if (!text.endsWith("s")) s"${text}s"
    else text
  }

}

case object Uppercase extends ReplaceOp {

  def apply(text: String): String = text.toUpperCase(Locale.US)

}

case object Lowercase extends ReplaceOp {

  def apply(text: String): String = text.toLowerCase(Locale.US)

}

case object LowerCamelcase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    if (textChars.nonEmpty) {
      textChars(0) = Character.toLowerCase(textChars(0))
      new String(textChars)
    } else {
      ""
    }
  }

}

case object UpperCamelcase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    if (textChars.nonEmpty) {
      textChars(0) = Character.toUpperCase(textChars(0))
      new String(textChars)
    } else {
      ""
    }
  }

}

case object LowerUnderscorecase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    textChars.foldLeft("") { (txt, char) =>
      if (char.isUpper && txt.nonEmpty) s"${txt}_${char.toLower}"
      else s"$txt$char"
    }
  }

}

case object UpperUnderscorecase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    textChars.foldLeft("") { (txt, char) =>
      if (char.isUpper && txt.nonEmpty) s"${txt}_$char"
      else s"$txt${char.toUpper}"
    }
  }

}

case object LowerHyphencase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    textChars.foldLeft("") { (txt, char) =>
      if (char.isUpper && txt.nonEmpty) s"$txt-${char.toLower}"
      else s"$txt$char"
    }
  }

}

case object UpperHyphencase extends ReplaceOp {

  def apply(text: String): String = {
    val textChars = text.toCharArray
    textChars.foldLeft("") { (txt, char) =>
      if (char.isUpper && txt.nonEmpty) s"$txt-$char"
      else s"$txt${char.toUpper}"
    }
  }

}

case object NoOp extends ReplaceOp {

  def apply(text: String): String = text

}

case class ReplaceString(toReplace: String, matchString: String, operations: Seq[ReplaceOp], partial: Boolean) {

  def transformReplacementString(replacement: String): String =
    operations.foldLeft(replacement) { (repl, op) =>
      op(repl)
    }

}

case object ReplaceString {

  def apply(toReplaceInput: String, matchStringWithOps: String, partial: Boolean): ReplaceString = {
    val matchStringParts: Seq[String] = matchStringWithOps.split('|').toSeq.map(_.trim)
    val (matchString, ops) =
      if (matchStringParts.nonEmpty) (matchStringParts.head, matchStringParts.tail.map(ReplaceOp(_)))
      else ("", Seq.empty[ReplaceOp])
    ReplaceString(toReplace = toReplaceInput, matchString = matchString, operations = ops, partial = partial)
  }

}
