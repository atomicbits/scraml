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

import io.atomicbits.scraml.ramlparser.parser.{RamlParseException, ParseContext}
import play.api.libs.json.{JsArray, JsObject, JsValue}

import scala.util.{Success, Failure, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Traits(traitsMap: Map[String, JsObject])


object Traits {

  def apply(): Traits = Traits(Map.empty[String, JsObject])


  def apply(traitsJson: JsValue)(implicit parseContext: ParseContext): Try[Traits] = {

    def doApply(trsJson: JsValue): Try[Traits] = {
      trsJson match {
        case JsInclude(included, source) => doApply(included)
        case traitsJsObj: JsObject       => Success(Traits(traitsJsObjToTraitMap(traitsJsObj)))
        case traitsJsArr: JsArray        =>
          val maps = traitsJsArr.value.collect {
            case traitsJsObj: JsObject => traitsJsObjToTraitMap(traitsJsObj)
          } reduce (_ ++ _)
          Success(Traits(maps))
        case x                           =>
          Failure(RamlParseException(s"The traits definition in ${parseContext.source} is malformed."))
      }
    }


    def traitsJsObjToTraitMap(traitsJsObj: JsObject): Map[String, JsObject] = {
      traitsJsObj.fields.collect {
        case (key: String, JsInclude(included: JsObject, source)) => key -> included
        case (key: String, value: JsObject)                       => key -> value
      }.toMap
    }

    doApply(traitsJson)
  }

}
