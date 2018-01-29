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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.{ ActionGenerator, DslSourceRewriter, GenerationAggr, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ ClassPointer, ClientClassDefinition }
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 14/01/17.
  */
case class ClientClassGenerator(scalaPlay: ScalaPlay) extends SourceGenerator {

  implicit val platform: ScalaPlay = scalaPlay

  def generate(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr = {

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val apiPackage        = clientClassDefinition.classReference.safePackageParts
    val apiClassName      = clientClassDefinition.classReference.name
    val apiClassReference = clientClassDefinition.classReference

    val (importClasses, dslFields, actionFunctions, headerPathSourceDefs) =
      clientClassDefinition.topLevelResourceDefinitions match {
        case oneRoot :: Nil if oneRoot.resource.urlSegment.isEmpty =>
          val dslFields = oneRoot.childResourceDefinitions.map(ResourceClassGenerator(platform).generateResourceDslField)
          val SourceCodeFragment(importClasses, actionFunctions, headerPathSourceDefs) =
            ActionGenerator(ScalaActionCodeGenerator(platform)).generateActionFunctions(oneRoot)
          (importClasses, dslFields, actionFunctions, headerPathSourceDefs)
        case manyRoots =>
          val importClasses   = Set.empty[ClassPointer]
          val dslFields       = manyRoots.map(ResourceClassGenerator(platform).generateResourceDslField)
          val actionFunctions = List.empty[String]
          (importClasses, dslFields, actionFunctions, List.empty)
      }

    val importStatements: Set[String] = platform.importStatements(apiClassReference, importClasses)

    val sourcecode =
      s"""
         package ${apiPackage.mkString(".")}

         import $dslBasePackage.client.{ClientFactory, ClientConfig}
         import $dslBasePackage.RestException
         import $dslBasePackage.RequestBuilder
         import $dslBasePackage.client.ning.Ning19ClientFactory
         import java.net.URL
         import play.api.libs.json._
         import java.io._

         ${importStatements.mkString("\n")}


         class $apiClassName(private val _requestBuilder: RequestBuilder) {

           import $dslBasePackage._

           ${dslFields.mkString("\n\n")}

           ${actionFunctions.mkString("\n\n")}

           def close() = _requestBuilder.client.close()

         }

         object $apiClassName {

           import $dslBasePackage.Response
           import play.api.libs.json._

           import scala.concurrent.ExecutionContext.Implicits.global
           import scala.concurrent.Future

           def apply(url:URL, config:ClientConfig=ClientConfig(), defaultHeaders:Map[String,String] = Map(), clientFactory: Option[ClientFactory] = None) : $apiClassName = {

             val requestBuilder =
               RequestBuilder(
                 clientFactory.getOrElse(Ning19ClientFactory)
                   .createClient(
                     protocol = url.getProtocol,
                     host = url.getHost,
                     port = if (url.getPort == -1) url.getDefaultPort else url.getPort,
                     prefix = if (url.getPath.isEmpty) None else Some(url.getPath),
                     config = config,
                     defaultHeaders = defaultHeaders
                   ).get
               )

             new $apiClassName(requestBuilder)
           }


           def apply(host: String,
                     port: Int,
                     protocol: String,
                     prefix: Option[String],
                     config: ClientConfig,
                     defaultHeaders: Map[String, String],
                     clientFactory: Option[ClientFactory]) = {

             val requestBuilder =
               RequestBuilder(
                 clientFactory.getOrElse(Ning19ClientFactory)
                   .createClient(
                     protocol = protocol,
                     host = host,
                     port = port,
                     prefix = prefix,
                     config = config,
                     defaultHeaders = defaultHeaders
                   ).get
               )

             new $apiClassName(requestBuilder)
             }


           implicit class FutureResponseOps[T](val futureResponse: Future[Response[T]]) extends AnyVal {

             def asString: Future[String] = futureResponse.map { resp =>
               resp.stringBody getOrElse {
                 val message =
                   if (resp.status != 200) s"The response has no string body because the request was not successful (status = $${resp.status})."
                   else "The response has no string body despite status 200."
                 throw new RestException(message, resp.status)
               }
             }

             def asJson: Future[JsValue] =
               futureResponse.map { resp =>
                 resp.jsonBody.getOrElse {
                   val message =
                     if (resp.status != 200) s"The response has no JSON body because the request was not successful (status = $${resp.status})."
                     else "The response has no JSON body despite status 200."
                   throw new RestException(message, resp.status)
                 }
               }

             def asType: Future[T] =
               futureResponse.map { resp =>
                 resp.body.getOrElse {
                   val message =
                     if (resp.status != 200) s"The response has no typed body because the request was not successful (status = $${resp.status})."
                     else "The response has no typed body despite status 200."
                   throw new RestException(message, resp.status)
                 }
               }

           }

         }
       """

    generationAggr
      .addSourceDefinitions(headerPathSourceDefs)
      .addSourceFile(SourceFile(filePath = apiClassReference.toFilePath, content = sourcecode))
  }

}
