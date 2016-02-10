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

import io.atomicbits.scraml.ramlparser.parser.RamlJsonParser
import play.api.libs.json.{JsString, JsValue}

/**
  * Created by peter on 10/02/16.
  */
object JsInclude {

  /**
    *
    * @param includeLinkObj e.g. { "!include": "types/collection.raml" }
    * @return
    */
  def unapply(includeLinkObj: JsValue): Option[JsValue] = {

    (includeLinkObj \ "!include").toOption.collect {
      case JsString(includeFile) =>
        RamlJsonParser.parseToJson(includeFile) match {
          case JsInclude(jsVal) => jsVal // handle recursive inclusions (although no sane person will ever use that)
          case x                => x
        }
    }

  }

}
