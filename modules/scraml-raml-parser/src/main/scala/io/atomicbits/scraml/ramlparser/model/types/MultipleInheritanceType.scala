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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{JsArray, JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.util.{Success, Try}

/**
  * Created by peter on 1/11/16.
  */
case class MultipleInheritanceType(parents: List[TypeReference],
                                   required: Option[Boolean] = None,
                                   model: TypeModel = RamlModel,
                                   id: Id = ImplicitId) extends NonePrimitiveType with AllowedAsObjectField {


  override def updated(updatedId: Id): MultipleInheritanceType = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): Type = copy(model = typeModel, parents = parents.map(_.asTypeModel(typeModel)))

  def asRequired = copy(required = Some(true))

}


object MultipleInheritanceType {


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[MultipleInheritanceType]] = {

    def processParentReferences(parents: Seq[JsValue]): Option[List[TypeReference]] = {
      val parentRefs =
        parents.collect {
          case JsString(parentRef) => Type(parentRef)
        }
      val typeReferences =
        parentRefs.collect {
          case Success(typeReference: TypeReference) => typeReference
        }

      if (typeReferences.size > 1) Some(typeReferences.toList)
      else None
    }


    (Type.typeDeclaration(json), json) match {
      case (Some(JsArray(parentReferences)), _) =>
        processParentReferences(parentReferences).map { parents =>
          val required = json.fieldBooleanValue("required")
          Success(MultipleInheritanceType(parents, required))
        }
      case (_, JsArray(parentReferences))       =>
        processParentReferences(parentReferences).map(parents => Success(MultipleInheritanceType(parents)))
      case _                                    => None
    }

  }

}
