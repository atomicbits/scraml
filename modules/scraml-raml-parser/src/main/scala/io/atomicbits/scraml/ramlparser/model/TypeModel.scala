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

import play.api.libs.json.{JsString, JsValue}

/**
  * Created by peter on 26/09/16.
  */
sealed trait TypeModel

case object RamlModel extends TypeModel

case object JsonSchemaModel extends TypeModel


object TypeModel {

  def apply(json: JsValue): TypeModel = {
    (json \ "$schema").toOption.collect {
      case JsString(schema) if schema.contains("http://json-schema.org") => JsonSchemaModel
    } getOrElse RamlModel
  }

}
