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

package io.atomicbits.scraml.ramlparser.parser

import play.api.libs.json.{JsString, JsValue, JsObject}

/**
  * Created by peter on 26/02/16.
  */
object Sourced {

  val sourcefield = "_source"

  /**
    * Unwraps a JSON object that has a "_source" field into the source value and the original json object.
    */
  def unapply(json: JsValue): Option[(JsObject, String)] = {

    (json \ sourcefield).toOption.collect {
      case JsString(includeFile) => (json.asInstanceOf[JsObject], includeFile)
    }
  }

}
