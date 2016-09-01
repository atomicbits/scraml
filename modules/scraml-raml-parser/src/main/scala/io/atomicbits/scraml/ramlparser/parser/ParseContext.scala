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

import io.atomicbits.scraml.ramlparser.model.{Id, Traits}
import play.api.libs.json.{JsString, JsValue}


/**
  * Created by peter on 10/02/16.
  */
case class ParseContext(var sourceTrail: List[String], nameToId: String => Id, traits: Traits = Traits()) {


  def withSource[T](jsValue: JsValue)(fn: => T): T = {
    (jsValue \ Sourced.sourcefield).toOption.collect {
      case JsString(source) =>
        val sourceTrailOrig = sourceTrail
        sourceTrail = source :: sourceTrailOrig
        val result = fn
        sourceTrail = sourceTrailOrig
        result
    } getOrElse fn
  }


  def head = sourceTrail.head

}
