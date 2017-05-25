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
import play.api.libs.json.{ JsArray, JsObject, JsValue }

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 25/05/17.
  */
case class ResourceTypes(resourceTypesMap: Map[String, JsObject]) extends ModelMerge {

  def applyToResource[T](jsObject: JsObject)(f: JsObject => Try[T])(implicit parseContext: ParseContext): Try[T] = {
    val resourceTypeName = findResourceTypeName(jsObject)
    applyToForMergeNames(jsObject, resourceTypeName, resourceTypesMap).flatMap(f)
  }

  def findResourceTypeName(jsObject: JsObject): Seq[String] = {
    jsObject.fieldStringValue("type").map(List(_)).getOrElse(List.empty)
  }

}

object ResourceTypes {

  def apply(): ResourceTypes = ResourceTypes(Map.empty[String, JsObject])

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[ResourceTypes] = {

    def doApply(rtJson: JsValue): Try[ResourceTypes] = {
      rtJson match {
        case rtJsObj: JsObject => Success(ResourceTypes(resourceTypesJsObjToResourceTypeMap(rtJsObj)))
        case rtJsArr: JsArray  => Success(ResourceTypes(resourceTypesJsObjToResourceTypeMap(KeyedList.toJsObject(rtJsArr))))
        case x =>
          Failure(
            RamlParseException(s"The resourceTypes definition in ${parseContext.head} is malformed.")
          )
      }
    }

    def resourceTypesJsObjToResourceTypeMap(resourceTypesJsObj: JsObject): Map[String, JsObject] = {
      resourceTypesJsObj.fields.collect {
        case (key: String, value: JsObject) => key -> value
      }.toMap
    }

    parseContext.withSource(json)(doApply(json))
  }

}
