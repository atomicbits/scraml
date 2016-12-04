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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model.NativeId
import io.atomicbits.scraml.ramlparser.parser.{KeyedList, ParseContext, RamlParseException}
import play.api.libs.json.{JsArray, JsObject, JsValue}

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Types(typeReferences: Map[NativeId, Type] = Map.empty) {

  def apply(nativeId: NativeId): Type = typeReferences(nativeId)

  def get(nativeId: NativeId): Option[Type] = typeReferences.get(nativeId)

  def ++(otherTypes: Types): Types = {
    Types(typeReferences ++ otherTypes.typeReferences)
  }

  def +(typeReference: (NativeId, Type)): Types = copy(typeReferences = typeReferences + typeReference)

}


object Types {

  def apply(typesJson: JsValue)(implicit parseContext: ParseContext): Try[Types] = {

    def doApply(tpsJson: JsValue): Try[Types] = {
      tpsJson match {
        case typesJsObj: JsObject => typesJsObjToTypes(typesJsObj)
        case typesJsArr: JsArray  => typesJsObjToTypes(KeyedList.toJsObject(typesJsArr))
        case x                    =>
          Failure(RamlParseException(s"The types (or schemas) definition in ${parseContext.head} is malformed."))
      }
    }

    def typesJsObjToTypes(typesJsObj: JsObject): Try[Types] = {
      val tryTypes =
        typesJsObj.fields.collect {
          case (key: String, incl: JsValue) => parseContext.withSource(incl)(typeObjectToType(key, incl))
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

    def typeObjectToType(name: String, typeDefinition: JsValue)(implicit parseContext: ParseContext): Try[Types] = {

      val result =
        typeDefinition match {
          case Type(sometype) => sometype.map(tp => Types(typeReferences = Map(NativeId(name) -> tp)))
          case x              => Failure(RamlParseException(s"Unknown type $x in ${parseContext.head}."))
        }

      result
    }

    parseContext.withSource(typesJson)(doApply(typesJson))
  }

}
