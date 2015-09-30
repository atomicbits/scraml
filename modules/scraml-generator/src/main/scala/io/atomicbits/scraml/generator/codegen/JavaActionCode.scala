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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.parser.model.Parameter

/**
 * Created by peter on 30/09/15.
 */
object JavaActionCode extends ActionCode {


  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String = {
    s"""public ${headerSegment.classRef.fullyQualifiedName} $contentHeaderMethodName =
          new ${headerSegment.classRef.fullyQualifiedName}(this.getRequestBuilder());"""
  }


  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String = {
    s"""
         package ${headerSegmentClassRef.packageName};

         import io.atomicbits.scraml.dsl.java.*;

         ${imports.mkString(";\n")};


         public class ${headerSegmentClassRef.name} extends HeaderSegment {

           public ${headerSegmentClassRef.name}(RequestBuilder requestBuilder) {
             super(requestBuilder);
           }

           ${methods.mkString("\n")}

         }
       """
  }


  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = {
    parameters map { parameterDef =>
      val(field, classRef) = parameterDef
      s"${classRef.classDefinitionJava} $field"
    }
  }


  def bodyTypes(action: RichAction): List[Option[ClassPointer]] = ???

  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String = ???

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter)): String = ???

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = ???

  def generateAction(action: RichAction,
                     segmentType: String,
                     actionParameters: List[String] = List.empty,
                     bodyField: Boolean = false,
                     queryParameterMapEntries: List[String] = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     multipartParams: Option[String] = None): String = ???

}
