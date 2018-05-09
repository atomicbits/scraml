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

import play.api.libs.json.{ JsObject, JsString, JsValue }

/**
  * Created by peter on 26/09/16.
  */
sealed trait TypeModel {

  def mark(json: JsValue): JsValue

}

case object RamlModel extends TypeModel {

  def mark(json: JsValue): JsValue = json

}

case object JsonSchemaModel extends TypeModel {

  val markerField = "$schema"

  val markerValue = "http://json-schema.org/draft-04/schema"

  def mark(json: JsValue): JsValue = json match {
    case jsObj: JsObject => jsObj + (markerField -> JsString(markerValue))
    case other           => other
  }

}

object TypeModel {

  def apply(json: JsValue): TypeModel = {
    (json \ "$schema").toOption.collect {
      case JsString(schema) if schema.contains("http://json-schema.org") => JsonSchemaModel
    } getOrElse RamlModel
  }

}
