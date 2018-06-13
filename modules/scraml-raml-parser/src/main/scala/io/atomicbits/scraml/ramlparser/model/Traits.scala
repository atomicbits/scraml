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

import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 10/02/16.
  */
case class Traits(traitsMap: Map[String, JsObject]) extends ModelMerge {

  /**
    * Apply the matching traits in this to the given JsObject by deep-copying the objects and
    * giving priority to the given jsObject when collisions should occur.
    */
  def applyToAction[T](jsObject: JsObject)(f: JsObject => Try[T])(implicit parseContext: ParseContext): Try[T] = {
    mergeInToAction(jsObject).flatMap(f)
  }

  def mergeInToAction(actionJsObj: JsObject)(implicit parseContext: ParseContext): Try[JsObject] = {
    val appliedTraits: MergeApplicationMap = findMergeNames(actionJsObj, Traits.selectionKey)
    applyToForMergeNames(actionJsObj, appliedTraits, traitsMap)
  }

  def mergeInToActionFromResource(actionJsObj: JsObject, resourceJsObj: JsObject)(implicit parseContext: ParseContext): Try[JsObject] = {
    val appliedTraits: MergeApplicationMap = findMergeNames(resourceJsObj, Traits.selectionKey)
    applyToForMergeNames(actionJsObj, appliedTraits, traitsMap)
  }

}

object Traits {

  val selectionKey: String = "is"

  def apply(): Traits = Traits(Map.empty[String, JsObject])

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[Traits] = {

    def doApply(trsJson: JsValue): Try[Traits] = {
      trsJson match {
        case traitsJsObj: JsObject => Success(Traits(traitsJsObjToTraitMap(traitsJsObj)))
        case traitsJsArr: JsArray  => Success(Traits(traitsJsObjToTraitMap(KeyedList.toJsObject(traitsJsArr))))
        case x =>
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

    parseContext.withSourceAndUrlSegments(json)(doApply(json))
  }

}
