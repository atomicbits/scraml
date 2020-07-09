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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.{ ActionGenerator, GenerationAggr, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.SourceGenerator
import io.atomicbits.scraml.generator.typemodel.{ ClassPointer, ClientClassDefinition }
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.platform.androidjavajackson.AndroidJavaJackson
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 1/03/17.
  */
case class ClientClassGenerator(javaJackson: CommonJavaJacksonPlatform) extends SourceGenerator {

  implicit val platform: CommonJavaJacksonPlatform = javaJackson

  def generate(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr = {

    val apiPackage        = clientClassDefinition.classReference.safePackageParts
    val apiClassName      = clientClassDefinition.classReference.name
    val apiClassReference = clientClassDefinition.classReference

    val (importClasses, dslFields, actionFunctions, headerPathSourceDefs) =
      clientClassDefinition.topLevelResourceDefinitions match {
        case oneRoot :: Nil if oneRoot.resource.urlSegment.isEmpty =>
          val dslFields = oneRoot.childResourceDefinitions.map(ResourceClassGenerator(platform).generateResourceDslField)
          val SourceCodeFragment(importClasses, actionFunctions, headerPathSourceDefs) =
            ActionGenerator(new JavaActionCodeGenerator(platform)).generateActionFunctions(oneRoot)
          (importClasses, dslFields, actionFunctions, headerPathSourceDefs)
        case manyRoots =>
          val importClasses   = Set.empty[ClassPointer]
          val dslFields       = manyRoots.map(ResourceClassGenerator(platform).generateResourceDslField)
          val actionFunctions = List.empty[String]
          (importClasses, dslFields, actionFunctions, List.empty)
      }

    val importStatements: Set[String] = platform.importStatements(apiClassReference, importClasses)

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val (defaultClientFactory, defaultClientImportStatement) =
      platform match {
        case android: AndroidJavaJackson =>
          val defaultCF  = "OkHttpScramlClientFactory"
          val defaultCIS = s"import $dslBasePackage.client.okhttp.$defaultCF;"
          (defaultCF, defaultCIS)
        case _ =>
          val defaultCF  = "Ning2ClientFactory"
          val defaultCIS = s"import $dslBasePackage.client.ning.$defaultCF;"
          (defaultCF, defaultCIS)
      }

    val sourcecode =
      s"""
           package ${apiPackage.mkString(".")};

           import $dslBasePackage.RequestBuilder;
           import $dslBasePackage.client.ClientConfig;
           import $dslBasePackage.client.ClientFactory;
           import $dslBasePackage.Client;
           $defaultClientImportStatement

           import java.util.*;
           import java.util.concurrent.CompletableFuture;
           import java.io.*;

           ${importStatements.mkString("\n")}

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
                   ClientFactory cFactory = clientFactory != null ? clientFactory : new $defaultClientFactory();
                   Client client = cFactory.createClient(host, port, protocol, prefix, clientConfig, defaultHeaders);
                   this._requestBuilder.setClient(client);
               }


               ${dslFields.mkString("\n\n")}

               ${actionFunctions.mkString("\n\n")}

               public RequestBuilder getRequestBuilder() {
                   return this._requestBuilder;
               }

               public void close() throws Exception {
                   this._requestBuilder.getClient().close();
               }

           }
         """

    generationAggr
      .addSourceDefinitions(clientClassDefinition.topLevelResourceDefinitions)
      .addSourceDefinitions(headerPathSourceDefs)
      .addSourceFile(SourceFile(filePath = apiClassReference.toFilePath, content = sourcecode))
  }

}
