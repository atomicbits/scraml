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

package io.atomicbits.scraml.generator.codegen.java

import io.atomicbits.scraml.generator.codegen.java.JavaActionGenerator.JavaActionFunctionResult
import io.atomicbits.scraml.generator.model.{ClassReference, ClassRep, RichResource}
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 25/09/15.
 */
object JavaResourceClassGenerator {

  def generateResourceClasses(apiClassName: String,
                              apiPackageName: List[String],
                              resources: List[RichResource]): List[ClassRep] = {

    def generateClientClass(topLevelResources: List[RichResource]): List[ClassRep] = {

      val (imports, dslFields, actionFunctions, headerPathClassReps) =
        resources match {
          case oneRoot :: Nil if oneRoot.urlSegment.isEmpty =>
            val dslFields = oneRoot.resources.map(generateResourceDslField)
            val JavaActionFunctionResult(imports, actionFunctions, headerPathClassReps) =
              JavaActionGenerator.generateActionFunctions(oneRoot)
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

           import io.atomicbits.scraml.dsl.java.RequestBuilder;
           import io.atomicbits.scraml.dsl.java.client.ClientConfig;
           import io.atomicbits.scraml.dsl.java.client.ning.NingClientSupport;

           import java.util.HashMap;
           import java.util.Map;

           public class $apiClassName {

               private String host;
               private int port;
               private String protocol;
               private Map<String, String> defaultHeaders;

               RequestBuilder requestBuilder = new RequestBuilder();

               public $apiClassName(String host,
                                    int port,
                                    String protocol,
                                    String prefix,
                                    ClientConfig clientConfig,
                                    Map<String, String> defaultHeaders) {
                   this.host = host;
                   this.port = port;
                   this.protocol = protocol;
                   this.defaultHeaders = defaultHeaders;

                   this.requestBuilder.setClient(new NingClientSupport(host, port, protocol, prefix, clientConfig, defaultHeaders));
                   this.requestBuilder.initializeChildren();
               }


               ${dslFields.mkString("\n\n")}

               ${actionFunctions.mkString("\n\n")}


               public Map<String, String> getDefaultHeaders() {
                   return defaultHeaders;
               }

               public String getHost() {
                   return host;
               }

               public int getPort() {
                   return port;
               }

               public String getProtocol() {
                   return protocol;
               }

               public void close() {
                   this.requestBuilder.getClient().close();
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

      val sourcecode =
        s"""
           package ${resource.classRep.packageName};

           import io.atomicbits.scraml.dsl.java.*;
           import java.util.*;

           ${imports.mkString("\n")}

           $classDefinition

             public $className(){
             }

             $resourceConstructor

             public $className addHeader(String key, String value) {
               $className $classNameCamel = this.shallowClone();
               $classNameCamel.requestBuilder = $classNameCamel.requestBuilder.cloneAddHeader(key, value);
               return $classNameCamel;
             }

             ${dslFields.mkString("\n\n")}

             ${actionFunctions.mkString("\n\n")}

             private $className shallowClone() {
               $className $classNameCamel = new $className();
               $classNameCamel.value = this.value;
               $classNameCamel.requestBuilder = this.requestBuilder;
               return $classNameCamel
             }

           }
         """

    }


    def generateParameterType(parameterType: ParameterType): String = {
      parameterType match {
        case StringType  => "String"
        case IntegerType => "long"
        case NumberType  => "double"
        case BooleanType => "boolean"
        case x           => sys.error(s"Unknown URL parameter type $x")
      }
    }


    def generateResourceDslField(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType)
          s"""
             public ${resource.classRep.fullyQualifiedName} ${resource.urlSegment}($paramType value) {
               return new ${resource.classRep.fullyQualifiedName}(value, this.getRequestBuilder());
             }
            """
        case None            =>
          s"""
              public ${resource.classRep.fullyQualifiedName} ${resource.urlSegment} =
                new ${resource.classRep.fullyQualifiedName}(this.requestBuilder);
            """
      }

    generateClientClass(resources) ::: resources.flatMap(generateResourceClassesHelper)
  }

}
