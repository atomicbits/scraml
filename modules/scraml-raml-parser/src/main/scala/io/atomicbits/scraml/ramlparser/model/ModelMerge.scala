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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json._
import io.atomicbits.scraml.util.TryUtils._

import scala.language.postfixOps
import scala.util.{ Failure, Try }

/**
  * Created by peter on 25/05/17.
  */
trait ModelMerge {

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

  def mergeDef: Map[String, JsValue] = substitutions.map(sub => (sub.name, sub.value)).toMap

  def get(name: String): Option[JsValue] = mergeDef.get(name)

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
