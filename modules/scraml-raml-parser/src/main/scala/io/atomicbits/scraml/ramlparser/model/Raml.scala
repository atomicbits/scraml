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

import io.atomicbits.scraml.ramlparser.parser.{TryUtils, RamlParseException, ParseContext}
import play.api.libs.json.{JsValue, JsArray, JsString, JsObject}

import scala.util.{Failure, Success, Try}

import TryUtils._

/**
  * Created by peter on 10/02/16.
  */
case class Raml(title: String,
                mediaType: Option[MimeType],
                description: Option[String],
                version: Option[String],
                baseUri: Option[String],
                baseUriParameters: Map[String, Parameter],
                protocols: Option[Seq[String]],
                traits: Traits,
                types: Types,
                resources: List[Resource])


object Raml {


  def apply(ramlJson: JsObject)(implicit parseContext: ParseContext): Try[Raml] = {

    // Process the properties
    val title: Try[String] =
      (ramlJson \ "title").toOption.collect {
        case JsString(t) => Success(t)
        case x           =>
          Failure(RamlParseException(s"File ${parseContext.sourceTrail} has a title field that is not a string value."))
      } getOrElse Failure(RamlParseException(s"File ${parseContext.sourceTrail} does not contain the mandatory title field."))


    val traits: Try[Traits] =
      (ramlJson \ "traits").toOption.map(Traits(_)).getOrElse(Success(Traits()))


    val types: Try[Types] = {
      List((ramlJson \ "types").toOption, (ramlJson \ "schemas").toOption).flatten match {
        case List(ts, ss) =>
          Failure(
            RamlParseException(
              s"File ${parseContext.sourceTrail} contains both a 'types' and a 'schemas' field. You should only use a 'types' field."
            )
          )
        case List(t)      => Types(t)
        case Nil          => Success(Types())
      }
    }


    val mediaType: Try[Option[MimeType]] = {
      (ramlJson \ "mediaType").toOption.collect {
        case JsString(mType) => Success(Option(MimeType(mType)))
        case x               => Failure(RamlParseException(s"The mediaType in ${parseContext.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }


    val description: Try[Option[String]] = {
      (ramlJson \ "description").toOption.collect {
        case JsString(docu) => Success(Option(docu))
        case x              =>
          Failure(RamlParseException(s"The description field in ${parseContext.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }


    val protocols: Try[Option[Seq[String]]] = {

      def toProtocolString(protocolString: JsValue): Try[String] = {
        protocolString match {
          case JsString(pString) => Success(pString)
          case x                 => Failure(RamlParseException(s"One of the protocols is not a string value."))
        }
      }

      (ramlJson \ "protocols").toOption.collect {
        case JsArray(pcols) => accumulate(pcols.map(toProtocolString)).map(Some(_))
        case x              =>
          Failure(RamlParseException(s"The documentation field in ${parseContext.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    val version: Try[Option[String]] = {
      (ramlJson \ "version").toOption.collect {
        case JsString(v) => Success(Option(v))
        case x           =>
          Failure(RamlParseException(s"The version field in ${parseContext.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    val baseUri: Try[Option[String]] = {
      (ramlJson \ "baseUri").toOption.collect {
        case JsString(v) => Success(Option(v))
        case x           =>
          Failure(RamlParseException(s"The baseUri field in ${parseContext.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }


    val baseUriParameters: Try[Map[String, Parameter]] = readUriParameters((ramlJson \ "baseUriParameters").toOption)


    def readUriParameters(jsParams: Option[JsValue]): Try[Map[String, Parameter]] = {
      jsParams.collect {
        case JsObject(jsObj) =>
          val paramTryList =
            jsObj.collect {
              case (paramName, paramProperties: JsObject) =>
                (paramProperties \ "type").toOption.collect {
                  case JsString("string")  => Success(paramName -> Parameter(parameterType = StringType, required = true))
                  case JsString("number")  => Success(paramName -> Parameter(parameterType = NumberType, required = true))
                  case JsString("integer") => Success(paramName -> Parameter(parameterType = IntegerType, required = true))
                  case JsString("boolean") => Success(paramName -> Parameter(parameterType = BooleanType, required = true))
                  case JsString("date")    => Success(paramName -> Parameter(parameterType = DateType, required = true))
                } getOrElse Success(paramName -> Parameter(parameterType = StringType, required = true))
              case (paramName, nonObjectProperties)       =>
                Failure(RamlParseException(s"Base URI parameter $paramName does not have a valid 'type' description."))
            }
          accumulate(paramTryList.toSeq).map(_.toMap)
        case x               => Failure(RamlParseException(s"The 'baseUriParameters' field should not be empty if present."))
      } getOrElse Success(Map.empty)
    }


    val resources: Try[List[Resource]] = Success(List.empty)

    //    val resourceTypes: Try[]
    //    val annotationTypes: Try[]

    /**
      * title
      * traits
      * types (schemas - deprecated)
      * mediaType
      * description
      * protocols
      * version
      * baseUri
      * baseUriParameters
      *
      * resourceTypes
      * annotationTypes
      * securedBy
      * securitySchemes
      * documentation
      * uses
      */


    withSuccess(
      title,
      mediaType,
      description,
      version,
      baseUri,
      baseUriParameters,
      protocols,
      traits,
      types,
      resources
    ) (Raml(_, _, _, _, _, _, _, _, _, _))
  }

}
