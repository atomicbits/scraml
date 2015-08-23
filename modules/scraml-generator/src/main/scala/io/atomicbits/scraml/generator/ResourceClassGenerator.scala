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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.lookup.SchemaLookup
import io.atomicbits.scraml.generator.model.RichResource
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 22/08/15. 
 */
object ResourceClassGenerator {


  def generateResourceClasses(resources: List[RichResource], schemaLookup: SchemaLookup): List[ClassRep] = {

    // A resource class needs to have one field path entry for each of its child resources. It needs to include the child resource's
    // (fully qualified) class. The resource's package needs to follow the (cleaned) rest path name to guarantee unique class names.
    // A resource class needs to have entries for each of its actions and including all the classes involved in that action definition.

    def generateClientClass(topLevelResources: List[RichResource]): ClassRep = {

      // ToDo...

      ???
    }

    def generateResourceClassesHelper(resource: RichResource): List[ClassRep] = {

      val classDefinition = generateClassDefinition(resource)

      val fieldImports = resource.resources.map(generateResourceFieldImports).toSet
      val dslFields = resource.resources.map(generateResourceDslField)

      val actionImports = resource.actions.flatMap(ActionGenerator.generateActionImports).toSet
      val actionFunctions = resource.actions.flatMap(ActionGenerator.generateActionFunctions)

      val imports = fieldImports ++ actionImports

      // ToDo: add copyright statement and license.
      val sourcecode =
        s"""
           package ${resource.packageParts.mkString(".")}

           import io.atomicbits.scraml.dsl.{PlainSegment, RequestBuilder}

           ${imports.mkString("\n")}

           $classDefinition

             def withHeader(header: (String, String)) =
               new ${resource.resourceClassName}(value, requestBuilder.withAddedHeaders(header))

             def withHeaders(newHeaders: (String, String)*) =
               new ${resource.resourceClassName}(value, requestBuilder.withAddedHeaders(newHeaders: _*))

           ${dslFields.mkString("\n\n")}

           ${actionFunctions.mkString("\n\n")}

           }
       """

      val resourceClassRep =
        ClassRep(
          name = resource.resourceClassName,
          packageParts = resource.packageParts,
          content = Some(sourcecode)
        )

      resourceClassRep :: resource.resources.flatMap(generateResourceClassesHelper)

    }

    def generateClassDefinition(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType)
          s"""class ${resource.resourceClassName}(value: $paramType, req: RequestBuilder) extends ParamSegment[$paramType](value, req) { """
        case None            =>
          s"""class ${resource.resourceClassName}(req: RequestBuilder) extends PlainSegment("${resource.urlSegment}", req) { """
      }

    def generateParameterType(parameterType: ParameterType): String = {
      parameterType match {
        case Parameter(StringType, _, _)  => "String"
        case Parameter(IntegerType, _, _) => "Long"
        case Parameter(NumberType, _, _)  => "Double"
        case Parameter(BooleanType, _, _) => "Boolean"
        case x                            => sys.error(s"Unknown URL parameter type $x")
      }
    }

    def generateResourceFieldImports(resource: RichResource): String = {
      s"import ${resource.packageParts.mkString(".")}.${resource.resourceClassName}"
    }

    def generateResourceDslField(resource: RichResource): String =
      s"""def ${resource.urlSegment} = new ${resource.resourceClassName}(requestBuilder.withAddedPathSegment("${resource.urlSegment}"))"""


    generateClientClass(resources) :: resources.flatMap(generateResourceClassesHelper)

  }


}
