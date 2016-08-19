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

import io.atomicbits.scraml.ramlparser.parser.{KeyedList, ParseContext, RamlParseException}
import play.api.libs.json.{JsArray, JsObject}

import scala.util.{Failure, Success, Try}

import io.atomicbits.scraml.ramlparser.parser.TryUtils._


/**
  * Created by peter on 10/02/16.
  */
case class Resource(urlSegment: String,
                    urlParameter: Option[Parameter] = None,
                    actions: List[Action] = List.empty,
                    resources: List[Resource] = List.empty,
                    parent: Option[Resource] = None)


object Resource {

  def apply(resourceUrl: String, jsObject: JsObject)(implicit parseContext: ParseContext): Try[Resource] = {

    implicit val newParseContext = parseContext.updateFrom(jsObject)


    def uriParamObjToParamMap(uriParamObj: JsObject): Try[Map[String, Parameter]] = {
      val paramMap =
        uriParamObj.value.toMap.collect {
          case (paramName, paramProperties: JsObject) => paramName -> Parameter.asUriParameter(paramProperties)
          case (paramName, paramProperties)           => paramName -> Try(Parameter(parameterType = StringType().asRequired))
        }
      accumulate(paramMap)
    }

    val uriParameterMap: Try[Map[String, Parameter]] =
      (jsObject \ "uriParameters").toOption.collect {
        case uriParamObj: JsObject => uriParamObjToParamMap(uriParamObj)
        case uriParamArr: JsArray  => uriParamObjToParamMap(KeyedList.toJsObject(uriParamArr))
        case x                     => Failure(RamlParseException(s"Empty uriParameters field given in ${parseContext.head}."))
      } getOrElse Success(Map.empty[String, Parameter])

    

  }

}