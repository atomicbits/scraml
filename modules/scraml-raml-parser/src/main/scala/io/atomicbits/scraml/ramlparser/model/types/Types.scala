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

package io.atomicbits.scraml.ramlparser.model.types

import io.atomicbits.scraml.ramlparser.model.Id
import io.atomicbits.scraml.ramlparser.parser.{KeyedList, ParseContext, RamlParseException, Sourced}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Types(nativeTypes: List[Type] = List.empty, external: Map[String, String] = Map.empty) {

  def ++(otherTypes: Types): Types = {
    Types(nativeTypes ++ otherTypes.nativeTypes, external ++ otherTypes.external)
  }

}


object Types {

  def apply(typesJson: JsValue)(implicit parseContext: ParseContext, nameToId: String => Id): Try[Types] = {

    // todo: check if we have to unapply the Sourced inclusion like this, or can we just use
    //  implicit val newParseContext = parseContext.updateFrom(typesJson)
    // here?

    def doApply(tpsJson: JsValue)(implicit parseContext: ParseContext): Try[Types] = {
      tpsJson match {
        case Sourced(included, source) => doApply(included)(parseContext.addSource(source))
        case typesJsObj: JsObject      => typesJsObjToTypes(typesJsObj)
        case typesJsArr: JsArray       => typesJsObjToTypes(KeyedList.toJsObject(typesJsArr))
        case x                         =>
          Failure(RamlParseException(s"The types (or schemas) definition in ${parseContext.head} is malformed."))
      }
    }


    def typesJsObjToTypes(typesJsObj: JsObject)(implicit parseContext: ParseContext): Try[Types] = {
      val tryTypes =
        typesJsObj.fields.collect {
          case (key: String, Sourced(included, source)) =>
            included match {
              case incl: JsObject  => typeObjectToNativeTypes(key, incl)(parseContext.addSource(source))
              case JsString(value) => Success(Types(external = Map(key -> value)))
            }
          case (key: String, JsString(value))           =>
            // json-schema is parsed as a single string because it was not in a yaml file
            Success(Types(external = Map(key -> value)))
          case (key: String, value: JsObject)           => typeObjectToNativeTypes(key, value)
        }
      foldTryTypes(tryTypes)
    }


    def foldTryTypes(tryTypes: Seq[Try[Types]])(implicit parseContext: ParseContext): Try[Types] = {
      tryTypes.foldLeft[Try[Types]](Success(Types())) {
        case (Success(aggr), Success(types))   => Success(aggr ++ types)
        case (fail@Failure(e), _)              => fail
        case (_, fail@Failure(e))              => fail
        case (Failure(eAggr), Failure(eTypes)) => Failure(RamlParseException(s"${eAggr.getMessage}\n${eTypes.getMessage}"))
      }
    }


    def typeObjectToNativeTypes(name: String, typeDefinition: JsObject)(implicit parseContext: ParseContext): Try[Types] = {

      (name, typeDefinition) match {
        case Type(sometype) => sometype.map(tp => Types(nativeTypes = List(tp)))
      }

    }


    doApply(typesJson)
  }

}
