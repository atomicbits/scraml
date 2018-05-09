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
import play.api.libs.json.{ JsArray, JsObject, JsValue }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 25/05/17.
  */
case class ResourceTypes(resourceTypesMap: Map[String, JsObject]) extends ModelMerge {

  def applyToResource[T](jsObject: JsObject)(f: JsObject => Try[T])(implicit parseContext: ParseContext): Try[T] = {
    val appliedResourceTypes: MergeApplicationMap = findMergeNames(jsObject, ResourceTypes.selectionKey)
    applyToForMergeNames(jsObject, appliedResourceTypes, resourceTypesMap, optionalTopLevelField = true).flatMap(f)
  }

}

object ResourceTypes {

  val selectionKey: String = "type"

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

    parseContext.withSourceAndUrlSegments(json)(doApply(json))
  }

}
