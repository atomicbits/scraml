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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import io.atomicbits.scraml.util.TryUtils
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import play.api.libs.json.{JsString, JsValue}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 1/11/16.
  */
case class UnionType(types: Set[Type],
                     required: Option[Boolean] = None,
                     model: TypeModel = RamlModel,
                     id: Id = ImplicitId) extends NonPrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): UnionType = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): Type = copy(model = typeModel, types = types.map(_.asTypeModel(typeModel)))

  def asRequired = copy(required = Some(true))

}


object UnionType {


  def unapply(unionExpression: String)(implicit parseContext: ParseContext): Option[Try[UnionType]] = {
    addUnionTypes(UnionType(Set.empty), unionExpression)
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[UnionType]] = {

    (Type.typeDeclaration(json), json) match {
      case (Some(JsString(unionExpression)), _) =>
        val required = json.fieldBooleanValue("required")
        addUnionTypes(UnionType(Set.empty, required), unionExpression)
      case (_, JsString(unionExpression))       =>
        addUnionTypes(UnionType(Set.empty), unionExpression)
      case _                                    => None
    }

  }


  private def addUnionTypes(unionType: UnionType, unionExpression: String)(implicit parseContext: ParseContext): Option[Try[UnionType]] = {
    typeExpressions(unionExpression).map { triedExpressions =>
      val triedTypes =
        triedExpressions.flatMap { stringExpressions =>
          TryUtils.accumulate(stringExpressions.map(Type(_)))
        }
      triedTypes.map { types =>
        unionType.copy(types = types.toSet)
      }
    }
  }


  private def typeExpressions(unionExpression: String)(implicit parseContext: ParseContext): Option[Try[List[String]]] = {

    val unionExprTrimmed = unionExpression.trim

    val unionExprUnwrapped: Try[String] =
      if (unionExprTrimmed.startsWith("(")) {
        if (unionExprTrimmed.endsWith(")")) Success(unionExprTrimmed.drop(1).dropRight(1))
        else Failure(RamlParseException(s"Union expression $unionExprTrimmed starts with a '(' but doesn't end with a ')'."))
      } else {
        Success(unionExprTrimmed)
      }

    val typeExpressions =
      unionExprUnwrapped.map { unionExpr =>
        unionExpr.split('|').toList.map(_.trim)
      }

    typeExpressions match {
      case Success(exp1 :: exp2 :: exps) => Some(typeExpressions)
      case Success(_)                    => None
      case Failure(exc)                  => Some(typeExpressions)
    }
  }


}
