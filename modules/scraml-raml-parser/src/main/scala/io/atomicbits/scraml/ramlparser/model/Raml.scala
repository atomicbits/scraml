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

import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{JsString, JsObject}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Raml(resources: List[Resource], schemas: Map[String, String])


object Raml {

  def apply(ramlJson: JsObject)(implicit parseContext: ParseContext): Try[Raml] = {

    // Process the properties
    val title =
      (ramlJson \ "title").toOption.collect {
        case JsString(t) => Success(t)
        case x           => Failure(new IllegalArgumentException(s"File ${parseContext.source} has a title field that is not a string value."))
      } getOrElse Failure(new IllegalArgumentException(s"File ${parseContext.source} does not contain the mandatory title field."))

    val traits =
      (ramlJson \ "traits").toOption.map(Traits(_)).getOrElse(Success(Traits()))

    val types = ???

    /**
      * title
      * traits
      * types (schemas - deprecated)
      *
      * resourceTypes
      * annotationTypes
      * mediaType
      *
      * documentation
      * securedBy
      * securitySchemes
      * protocols
      * baseUri
      * baseUriParameters
      * version
      * uses
      * description
      */

    ???
  }

}