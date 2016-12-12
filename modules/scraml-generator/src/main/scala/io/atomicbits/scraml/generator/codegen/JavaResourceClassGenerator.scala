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

import io.atomicbits.scraml.generator.model.{ClassReference, ClassRep, Language, RichResource}
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 25/09/15.
  */
object JavaResourceClassGenerator {

  def generateResourceClasses(apiClassName: String,
                              apiPackageName: List[String],
                              resources: List[RichResource])
                             (implicit lang: Language): List[ClassRep] = {

    def generateClientClass(topLevelResources: List[RichResource]): List[ClassRep] = {

      val (imports, dslFields, actionFunctions, headerPathClassReps) =
        resources match {
          case oneRoot :: Nil if oneRoot.urlSegment.isEmpty =>
            val dslFields = oneRoot.resources.map(generateResourceDslField)
            val ActionFunctionResult(imports, actionFunctions, headerPathClassReps) =
              ActionGenerator(JavaActionCode).generateActionFunctions(oneRoot)
            (imports, dslFields, actionFunctions, headerPathClassReps)
          case manyRoots                                    =>
            val imports = Set.empty[String]
            val dslFields = manyRoots.map(generateResourceDslField)
            val actionFunctions = List.empty[String]
            (imports, dslFields, actionFunctions, List.empty)
        }

      val sourcecode =
        s"""
           package ${apiPackageName.mkString(".")};

           import io.atomicbits.scraml.jdsl.RequestBuilder;
           import io.atomicbits.scraml.jdsl.client.ClientConfig;
           import io.atomicbits.scraml.jdsl.client.ClientFactory;
           import io.atomicbits.scraml.jdsl.Client;
           import io.atomicbits.scraml.jdsl.client.ning.Ning19ClientFactory;

           import java.util.*;
           import java.util.concurrent.CompletableFuture;
           import java.io.*;


           public class $apiClassName {

               private RequestBuilder _requestBuilder = new RequestBuilder();

               public $apiClassName(String host,
                                    int port,
                                    String protocol,
                                    String prefix,
                                    ClientConfig clientConfig,
                                    Map<String, String> defaultHeaders) {
                   this(host, port, protocol, prefix, clientConfig, defaultHeaders, null);
               }


               public $apiClassName(String host,
                                    int port,
                                    String protocol,
                                    String prefix,
                                    ClientConfig clientConfig,
                                    Map<String, String> defaultHeaders,
                                    ClientFactory clientFactory) {
                   ClientFactory cFactory = clientFactory != null ? clientFactory : new Ning19ClientFactory();
                   Client client = cFactory.createClient(host, port, protocol, prefix, clientConfig, defaultHeaders);
                   this._requestBuilder.setClient(client);
               }


               ${dslFields.mkString("\n\n")}

               ${actionFunctions.mkString("\n\n")}

               public RequestBuilder getRequestBuilder() {
                   return this._requestBuilder;
               }

               public void close() {
                   this._requestBuilder.getClient().close();
               }

           }
         """

      val clientClass =
        ClassRep(classReference = ClassReference(name = apiClassName, packageParts = apiPackageName), content = Some(sourcecode))

      clientClass :: headerPathClassReps
    }


    def generateResourceClassesHelper(resource: RichResource): List[ClassRep] = {

      val className = resource.classRep.name
      val classNameCamel = CleanNameUtil.camelCased(className)

      val dslFields = resource.resources.map(generateResourceDslField)

      val ActionFunctionResult(actionImports, actionFunctions, headerPathClassReps) =
        ActionGenerator(JavaActionCode).generateActionFunctions(resource)

      val imports = actionImports

      val classDefinition = generateClassDefinition(resource)

      val resourceConstructors = generateResourceConstructors(resource)

      //      val shallowCloneValueAssignment =
      //        if (isParameterized(resource)) s"$classNameCamel._value = this._value;"
      //        else ""

      val sourcecode =
        s"""
           package ${resource.classRep.packageName};

           import io.atomicbits.scraml.jdsl.*;
           import java.util.*;
           import java.util.concurrent.CompletableFuture;
           import java.io.*;

           ${imports.mkString("", ";\n", ";")}

           $classDefinition

             public $className(){
             }

             ${resourceConstructors.mkString("\n\n")}

             public $className addHeader(String key, String value) {
               $className $classNameCamel = new $className(getRequestBuilder(), true);
               $classNameCamel._requestBuilder.addHeader(key, value);
               return $classNameCamel;
             }

             public $className setHeader(String key, String value) {
               $className $classNameCamel = new $className(getRequestBuilder(), true);
               $classNameCamel._requestBuilder.setHeader(key, value);
               return $classNameCamel;
             }

             ${dslFields.mkString("\n\n")}

             ${actionFunctions.mkString("\n\n")}

           }
         """

      val resourceClassRep = resource.classRep.withContent(sourcecode)

      resourceClassRep :: resource.resources.flatMap(generateResourceClassesHelper) ::: headerPathClassReps
    }


    def generateResourceConstructors(resource: RichResource): List[String] =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          List(
            s"""
               public ${resource.classRep.name}($paramType value, RequestBuilder requestBuilder) {
                 super(value, requestBuilder);
               }
             """,
            s"""
               public ${resource.classRep.name}(RequestBuilder requestBuilder, Boolean noPath) {
                 super(requestBuilder);
               }
             """
          )
        case None            =>
          List(
            s"""
               public ${resource.classRep.name}(RequestBuilder requestBuilder) {
                 super("${resource.urlSegment}", requestBuilder);
               }
             """,
            s"""
             public ${resource.classRep.name}(RequestBuilder requestBuilder, Boolean noPath) {
               super(requestBuilder);
             }
           """
          )
      }


    def generateClassDefinition(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          s"""public class ${resource.classRep.name} extends ParamSegment<$paramType> { """
        case None            =>
          s"""public class ${resource.classRep.name} extends PlainSegment {"""
      }


    def isParameterized(resource: RichResource) = resource.urlParameter.isDefined


    def generateParameterType(parameterType: ParsedType): String = {
      parameterType match {
        case stringType: ParsedString   => "String"
        case integerType: ParsedInteger => "Long" // NOT long
        case numberType: ParsedNumber   => "Double" // NOT double
        case booleanType: ParsedBoolean => "Boolean" // NOT boolean
        case x                          => sys.error(s"Unknown URL parameter type $x")
      }
    }


    def generateResourceDslField(resource: RichResource): String = {

      import CleanNameUtil._

      val cleanUrlSegment = escapeJavaKeyword(cleanMethodName(resource.urlSegment))
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          s"""
             public ${resource.classRep.fullyQualifiedName} $cleanUrlSegment($paramType value) {
               return new ${resource.classRep.fullyQualifiedName}(value, this.getRequestBuilder());
             }
            """
        case None            =>
          s"""
              public ${resource.classRep.fullyQualifiedName} $cleanUrlSegment =
                new ${resource.classRep.fullyQualifiedName}(this.getRequestBuilder());
            """
      }
    }

    generateClientClass(resources) ::: resources.flatMap(generateResourceClassesHelper)
  }

}
