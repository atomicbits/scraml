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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{ JsObject, JsValue }

import scala.util.{ Try }

/**
  * Created by peter on 22/12/16.
  */
object ParsedFragmentContainer {

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Fragments]] = {

    (ParsedType.typeDeclaration(json), (json \ "properties").toOption, json) match {
      case (None, None, jsObj: JsObject) => Some(Fragments(jsObj))
      case _                             => None
    }

  }

}
