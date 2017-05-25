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

import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json.{ JsArray, JsObject, JsValue }

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

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
    val traitNames = findTraitNames(actionJsObj)
    applyToForMergeNames(actionJsObj, traitNames, traitsMap)
  }

  def mergeInToActionFromResource(actionJsObj: JsObject, resourceJsObj: JsObject)(implicit parseContext: ParseContext): Try[JsObject] = {
    val traitNames = findTraitNames(resourceJsObj)
    applyToForMergeNames(actionJsObj, traitNames, traitsMap)
  }

  def findTraitNames(jsObject: JsObject): Seq[String] = jsObject.fieldStringListValue("is").getOrElse(List.empty)

}

object Traits {

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

    parseContext.withSource(json)(doApply(json))
  }

}
