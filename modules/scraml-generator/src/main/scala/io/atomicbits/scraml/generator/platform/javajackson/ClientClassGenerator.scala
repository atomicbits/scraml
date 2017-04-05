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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.{ SourceCodeFragment, ActionGenerator, GenerationAggr }
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaActionCodeGenerator
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ ClassPointer, ClientClassDefinition, SourceFile }
import io.atomicbits.scraml.generator.platform.Platform._

/**
  * Created by peter on 1/03/17.
  */
object ClientClassGenerator extends SourceGenerator {

  implicit val platform: Platform = JavaJackson

  def generate(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr = {

    val apiPackage        = clientClassDefinition.classReference.safePackageParts
    val apiClassName      = clientClassDefinition.classReference.name
    val apiClassReference = clientClassDefinition.classReference

    val (importClasses, dslFields, actionFunctions, headerPathSourceDefs) =
      clientClassDefinition.topLevelResourceDefinitions match {
        case oneRoot :: Nil if oneRoot.resource.urlSegment.isEmpty =>
          val dslFields = oneRoot.childResourceDefinitions.map(ResourceClassGenerator.generateResourceDslField)
          val SourceCodeFragment(importClasses, actionFunctions, headerPathSourceDefs) =
            ActionGenerator(ScalaActionCodeGenerator).generateActionFunctions(oneRoot)
          (importClasses, dslFields, actionFunctions, headerPathSourceDefs)
        case manyRoots =>
          val importClasses   = Set.empty[ClassPointer]
          val dslFields       = manyRoots.map(ResourceClassGenerator.generateResourceDslField)
          val actionFunctions = List.empty[String]
          (importClasses, dslFields, actionFunctions, List.empty)
      }

    val importStatements: Set[String] = platform.importStatements(apiClassReference, importClasses)

    val sourcecode =
      s"""
           package ${apiPackage.mkString(".")};

           import io.atomicbits.scraml.jdsl.RequestBuilder;
           import io.atomicbits.scraml.jdsl.client.ClientConfig;
           import io.atomicbits.scraml.jdsl.client.ClientFactory;
           import io.atomicbits.scraml.jdsl.Client;
           import io.atomicbits.scraml.jdsl.client.ning.Ning19ClientFactory;

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

    generationAggr
      .addSourceDefinitions(headerPathSourceDefs)
      .addSourceFile(SourceFile(filePath = apiClassReference.toFilePath, content = sourcecode))
  }

}
