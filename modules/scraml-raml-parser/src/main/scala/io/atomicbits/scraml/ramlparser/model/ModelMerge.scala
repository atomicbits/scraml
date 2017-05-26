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

import io.atomicbits.scraml.ramlparser.parser.{ ParseContext, RamlParseException }
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import io.atomicbits.scraml.util.TryUtils._

import scala.util.{ Failure, Try }

/**
  * Created by peter on 25/05/17.
  */
trait ModelMerge {

  def applyToForMergeNames(jsObject: JsObject,
                           mergeNames: Seq[String],
                           mergeMap: Map[String, JsObject],
                           optionalTopLevelField: Boolean = false)(implicit parseContext: ParseContext): Try[JsObject] = {

    val toMerge: Try[Seq[JsObject]] =
      accumulate(
        mergeNames.map { mergeName =>
          Try(mergeMap(mergeName))
            .recoverWith {
              case e => Failure(RamlParseException(s"Unknown trait or resourceType name $mergeName in ${parseContext.head}."))
            }
        }
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
