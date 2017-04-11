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
