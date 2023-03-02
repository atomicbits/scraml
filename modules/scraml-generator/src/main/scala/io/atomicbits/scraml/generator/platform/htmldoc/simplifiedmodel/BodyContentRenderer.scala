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

package io.atomicbits.scraml.generator.platform.htmldoc.simplifiedmodel

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._

/**
  * Created by peter on 11/06/18.
  */
case class BodyContentRenderer(generationAggr: GenerationAggr) {

  def renderHtmlForType(typeReference: TypeReference,
                        recursiveCanonicals: Set[CanonicalName]             = Set.empty,
                        typeParameterMap: Map[TypeParameter, TypeReference] = Map.empty,
                        suffix: String                                      = "",
                        required: Option[Boolean]                           = None,
                        description: Option[String]                         = None,
                        expandedLevels: Int                                 = 5): String = {

    def getSuffixes(items: List[_]): List[String] = {
      items match {
        case Nil       => List.empty
        case it :: Nil => List("")
        case it :: its => ", " :: getSuffixes(its)
      }
    }

    val comment =
      (required, description) match {
        case (Some(true), Some(desc)) => s" // (required) $desc"
        case (_, Some(desc))          => s" // $desc"
        case (_, None)                => ""
      }

    typeReference match {
      case BooleanType => s"""<span class="primitivetype">boolean$suffix$comment</span>"""
      case StringType  => s"""<span class="primitivetype">string$suffix$comment</span>"""
      case JsonType    => s"""<span class="primitivetype">json$suffix$comment</span>"""
      case IntegerType => s"""<span class="primitivetype">integer$suffix$comment</span>"""
      case NumberType  => s"""<span class="primitivetype">number$suffix$comment</span>"""
      case NullType    => s"""<span class="primitivetype">null$suffix$comment</span>"""
      case FileType    => s"""<span class="primitivetype">file$suffix$comment</span>"""
      case DateTimeDefaultType =>
        s"""<span class="primitivetype">datetime (rfc3339: yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX)$suffix$comment</span>"""
      case DateTimeRFC2616Type =>
        s"""<span class="primitivetype">datetime (rfc2616: EEE, dd MMM yyyy HH:mm:ss 'GMT')$suffix$comment</span>"""
      case DateTimeOnlyType => s"""<span class="primitivetype">datetime-only (yyyy-MM-dd'T'HH:mm:ss[.SSS])$suffix$comment</span>"""
      case TimeOnlyType     => s"""<span class="primitivetype">time-only (HH:mm:ss[.SSS])$suffix$comment</span>"""
      case DateOnlyType     => s"""<span class="primitivetype">date-only (yyyy-MM-dd)$suffix$comment</span>"""
      case ArrayTypeReference(genericType) =>
        genericType match {
          case typeReference: TypeReference =>
            s"""
               |<span>[</span>${renderHtmlForType(typeReference, recursiveCanonicals)}<span>]$suffix$comment</span>
             """.stripMargin
          case TypeParameter(paramName) => s"type parameter$suffix$comment"
        }
      case NonPrimitiveTypeReference(refers, genericTypes, genericTypeParameters) =>
        ((refers, generationAggr.canonicalToMap.get(refers)): @unchecked) match {
          case (ref, _) if recursiveCanonicals.contains(ref) => "" // ToDo: render recursive object type.
          case (ref, None)                                   => ""
          case (ref, Some(objectType: ObjectType)) =>
            val tpMap =
              genericTypeParameters
                .zip(genericTypes)
                .collect {
                  case (tp, tr: TypeReference) => (tp, tr)
                  // We don't consider transitive type parameters yet, that's why we ignore the case for
                  // (tp1, tp2: TypeParameter) for now.  (ToDo: https://github.com/atomicbits/scraml/issues/37)
                }
                .toMap

            // objectType.typeDiscriminator // ToDo
            // objectType.typeDiscriminatorValue // ToDo

            val recCanonicals = recursiveCanonicals + refers

            val propertyList           = objectType.properties.values.toList
            val suffixes               = getSuffixes(propertyList)
            val propertiesWithSuffixes = propertyList.zip(suffixes)

            val renderedProperties =
              propertiesWithSuffixes.map {
                case (property, suffx) => renderProperty(property, recCanonicals, tpMap, suffx, expandedLevels - 1)
              }

            s"""
               |<span>{</span>
               |    ${renderedProperties.mkString}
               |<span>}$suffix</span>
             """.stripMargin
          case (ref, Some(enumType: EnumType)) =>
            s"""<span class="primitivetype">${enumType.choices.map(CleanNameUtil.quoteString).mkString(" | ")}$suffix$comment</span>"""
          case (ref, Some(union: UnionType)) => "ToDo: render union types"
        }
      case other => s"Unknown type representation for $other$suffix$comment"
    }

  }

  private def renderProperty(property: Property[_],
                             recCanonicals: Set[CanonicalName],
                             tpMap: Map[TypeParameter, TypeReference],
                             propSuffix: String,
                             expLevels: Int): String = {

    val renderedType = {
      val typeReferenceOpt: Option[TypeReference] =
        property.ttype match {
          case tr: TypeReference => Some(tr)
          case tp: TypeParameter => tpMap.get(tp)
        }
      typeReferenceOpt.map { tr =>
        renderHtmlForType(
          typeReference       = tr,
          recursiveCanonicals = recCanonicals,
          typeParameterMap    = tpMap,
          suffix              = propSuffix,
          required            = Some(property.required),
          description         = None, // property.description // Todo: add the description to the parsed properties
          expandedLevels      = expLevels
        )
      } getOrElse ""
    }

    s"""
       |<div class='indent'>
       |  <span>\"${property.name}\":</span>$renderedType
       |</div>
     """.stripMargin
  }

}
