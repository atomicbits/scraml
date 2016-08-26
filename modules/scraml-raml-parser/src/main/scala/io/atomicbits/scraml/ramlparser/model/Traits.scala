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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.{KeyedList, ParseContext, RamlParseException, Sourced}
import play.api.libs.json.{JsArray, JsObject, JsUndefined, JsValue}

import scala.util.{Failure, Success, Try}
import io.atomicbits.scraml.ramlparser.parser.TryUtils._
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 10/02/16.
  */
case class Traits(traitsMap: Map[String, JsObject]) {

  /**
    * Apply the matching traits in this to the given JsObject by deep-copying the objects and
    * giving priority to the given jsObject when collisions should occur.
    */
  def applyTo[T](jsObject: JsObject)(f: JsObject => Try[T])(implicit parseContext: ParseContext): Try[T] = {

    val traitNames = jsObject.fieldStringListValue("is").getOrElse(List.empty)

    val tryTraits: Try[Seq[JsObject]] =
      accumulate(
        traitNames.map { traitName =>
          Try(traitsMap(traitName))
            .recoverWith {
              case e => Failure(RamlParseException(s"Unknown trait name $traitName in ${parseContext.head}."))
            }
        }
      )

    val deepMerged =
      tryTraits.map { traits =>
        traits.foldLeft(jsObject) { (aggr, currentTrait) =>
          deepMerge(currentTrait, aggr)
        }
      }

    deepMerged.flatMap(f)
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
  private def deepMerge(source: JsObject, target: JsObject): JsObject = {

    /**
      * Deep merges the fieldWithValue into the aggr json object.
      */
    def mergeFieldInto(aggregatedTarget: JsObject, fieldWithValue: (String, JsValue)): JsObject = {

      val (field, value) = fieldWithValue

      (aggregatedTarget \ field).toOption match {
        case Some(aggrValue: JsObject) =>
          value match {
            case jsOb: JsObject => aggregatedTarget + (field -> deepMerge(jsOb, aggrValue))
            case _              => aggregatedTarget
          }
        case Some(aggrValue: JsArray)  =>
          value match {
            case jsArr: JsArray => aggregatedTarget + (field -> mergeArrays(jsArr, aggrValue))
            case _              => aggregatedTarget
          }
        case Some(aggrValue)           => aggregatedTarget
        case None                      => aggregatedTarget + (field -> value)
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


object Traits {


  def apply(): Traits = Traits(Map.empty[String, JsObject])


  def apply(traitsJson: JsValue)(implicit parseContext: ParseContext): Try[Traits] = {

    implicit val newParseContext = parseContext.updateFrom(traitsJson)

    def doApply(trsJson: JsValue): Try[Traits] = {
      trsJson match {
        case traitsJsObj: JsObject => Success(Traits(traitsJsObjToTraitMap(traitsJsObj)))
        case traitsJsArr: JsArray  => Success(Traits(traitsJsObjToTraitMap(KeyedList.toJsObject(traitsJsArr))))
        case x                     =>
          Failure(
            RamlParseException(s"The traits definition in ${parseContext.head} is malformed.")
          )
      }
    }


    def traitsJsObjToTraitMap(traitsJsObj: JsObject): Map[String, JsObject] = {
      traitsJsObj.fields.collect {
        case (key: String, value: JsObject) => key -> value
      }.toMap
    }

    doApply(traitsJson)
  }

}
