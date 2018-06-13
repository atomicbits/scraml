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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json.{ JsArray, JsObject, JsValue }

import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 10/02/16.
  */
case class Types(typeReferences: Map[NativeId, ParsedType] = Map.empty) {

  def apply(nativeId: NativeId): ParsedType = typeReferences(nativeId)

  def get(nativeId: NativeId): Option[ParsedType] = typeReferences.get(nativeId)

  def ++(otherTypes: Types): Types = {
    Types(typeReferences ++ otherTypes.typeReferences)
  }

  def +(typeReference: (NativeId, ParsedType)): Types = copy(typeReferences = typeReferences + typeReference)

}

object Types {

  def apply(typesJson: JsValue)(implicit parseContext: ParseContext): Try[Types] = {

    def doApply(tpsJson: JsValue): Try[Types] = {
      tpsJson match {
        case typesJsObj: JsObject => typesJsObjToTypes(typesJsObj)
        case typesJsArr: JsArray  => typesJsObjToTypes(KeyedList.toJsObject(typesJsArr))
        case x =>
          Failure(RamlParseException(s"The types (or schemas) definition in ${parseContext.head} is malformed."))
      }
    }

    def typesJsObjToTypes(typesJsObj: JsObject): Try[Types] = {
      val tryTypes =
        typesJsObj.fields.collect {
          case (key: String, incl: JsValue) => parseContext.withSourceAndUrlSegments(incl)(typeObjectToType(key, incl))
        }
      foldTryTypes(tryTypes)
    }

    def foldTryTypes(tryTypes: Seq[Try[Types]])(implicit parseContext: ParseContext): Try[Types] = {
      tryTypes.foldLeft[Try[Types]](Success(Types())) {
        case (Success(aggr), Success(types))   => Success(aggr ++ types)
        case (fail @ Failure(e), _)            => fail
        case (_, fail @ Failure(e))            => fail
        case (Failure(eAggr), Failure(eTypes)) => Failure(RamlParseException(s"${eAggr.getMessage}\n${eTypes.getMessage}"))
      }
    }

    def typeObjectToType(name: String, typeDefinition: JsValue)(implicit parseContext: ParseContext): Try[Types] = {
      typeDefinition match {
        case ParsedType(sometype) =>
          sometype.map { tp =>
            val nativeId = NativeId(name)
            val typeWithProperId =
              (tp.id, tp.model) match {
                case (ImplicitId, RamlModel) => tp.updated(nativeId)
                case _                       => tp
              }
            Types(typeReferences = Map(nativeId -> typeWithProperId))
          }
        case x => Failure(RamlParseException(s"Unknown type $x in ${parseContext.head}."))
      }
    }

    parseContext.withSourceAndUrlSegments(typesJson)(doApply(typesJson))
  }

}
