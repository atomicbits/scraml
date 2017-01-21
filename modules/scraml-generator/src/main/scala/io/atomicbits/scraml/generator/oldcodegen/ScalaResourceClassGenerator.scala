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

package io.atomicbits.scraml.generator.oldcodegen

import io.atomicbits.scraml.generator.oldmodel.{ ClassReference, ClassRep, Language, RichResource }
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 22/08/15.
  */
object ScalaResourceClassGenerator {

  def generateResourceClasses(apiClassName: String, apiPackageName: List[String], resources: List[RichResource])(
      implicit lang: Language): List[ClassRep] = {

    // A resource class needs to have one field path entry for each of its child resources. It needs to include the child resource's
    // (fully qualified) class. The resource's package needs to follow the (cleaned) rest path name to guarantee unique class names.
    // A resource class needs to have entries for each of its actions and including all the classes involved in that action definition.

    def generateClientClass(topLevelResources: List[RichResource]): List[ClassRep] = {

      val (imports, dslFields, actionFunctions, headerPathClassReps) =
        resources match {
          case oneRoot :: Nil if oneRoot.urlSegment.isEmpty =>
            val dslFields = oneRoot.resources.map(generateResourceDslField)
            val ActionFunctionResult(imports, actionFunctions, headerPathClassReps) =
              ActionGenerator(ScalaActionCode).generateActionFunctions(oneRoot)
            (imports, dslFields, actionFunctions, headerPathClassReps)
          case manyRoots =>
            val imports         = Set.empty[String]
            val dslFields       = manyRoots.map(generateResourceDslField)
            val actionFunctions = List.empty[String]
            (imports, dslFields, actionFunctions, List.empty)
        }

      val sourcecode =
        s"""
         package ${apiPackageName.mkString(".")}

         import io.atomicbits.scraml.dsl.client.{ClientFactory, ClientConfig}
         import io.atomicbits.scraml.dsl.RequestBuilder
         import io.atomicbits.scraml.dsl.client.ning.Ning19ClientFactory
         import java.net.URL
         import play.api.libs.json._
         import java.io._

         ${imports.mkString("\n")}


         class $apiClassName(private val _requestBuilder: RequestBuilder) {

           import io.atomicbits.scraml.dsl._

           ${dslFields.mkString("\n\n")}

           ${actionFunctions.mkString("\n\n")}

           def close() = _requestBuilder.client.close()

         }

         object $apiClassName {

           import io.atomicbits.scraml.dsl.Response
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
                 throw new IllegalArgumentException(message)
               }
             }

             def asJson: Future[JsValue] =
               futureResponse.map { resp =>
                 resp.jsonBody.getOrElse {
                   val message =
                     if (resp.status != 200) s"The response has no JSON body because the request was not successful (status = $${resp.status})."
                     else "The response has no JSON body despite status 200."
                   throw new IllegalArgumentException(message)
                 }
               }

             def asType: Future[T] =
               futureResponse.map { resp =>
                 resp.body.getOrElse {
                   val message =
                     if (resp.status != 200) s"The response has no typed body because the request was not successful (status = $${resp.status})."
                     else "The response has no typed body despite status 200."
                   throw new IllegalArgumentException(message)
                 }
               }

           }

         }
       """

      val clientClass =
        ClassRep(classReference = ClassReference(name = apiClassName, packageParts = apiPackageName), content = Some(sourcecode))

      clientClass :: headerPathClassReps
    }

    def generateResourceClassesHelper(resource: RichResource): List[ClassRep] = {

      val classDefinition = generateClassDefinition(resource)

      val dslFields = resource.resources.map(generateResourceDslField)

      val ActionFunctionResult(actionImports, actionFunctions, headerPathClassReps) = ActionGenerator(ScalaActionCode)
        .generateActionFunctions(resource)

      val imports = actionImports

      val addHeaderConstructorArgs = generateAddHeaderConstructorArguments(resource)
      val setHeaderConstructorArgs = generateSetHeaderConstructorArguments(resource)

      val sourcecode =
        s"""
           package ${resource.classRep.packageName}

           import io.atomicbits.scraml.dsl._

           import play.api.libs.json._
           import java.io._

           ${imports.mkString("\n")}

           $classDefinition

           /**
            * addHeaders will add the given headers and append the values for existing headers.
            */
           def addHeaders(newHeaders: (String, String)*) =
             new ${resource.classRep.name}$addHeaderConstructorArgs

           /**
            * setHeaders will add the given headers and set (overwrite) the values for existing headers.
            */
           def setHeaders(newHeaders: (String, String)*) =
             new ${resource.classRep.name}$setHeaderConstructorArgs

           ${dslFields.mkString("\n\n")}

           ${actionFunctions.mkString("\n\n")}

           }
       """

      val resourceClassRep = resource.classRep.withContent(sourcecode)

      resourceClassRep :: resource.resources.flatMap(generateResourceClassesHelper) ::: headerPathClassReps
    }

    def generateClassDefinition(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          s"""class ${resource.classRep.name}(_value: $paramType, _req: RequestBuilder) extends ParamSegment[$paramType](_value, _req) { """
        case None =>
          s"""class ${resource.classRep.name}(private val _req: RequestBuilder) extends PlainSegment("${resource.urlSegment}", _req) { """
      }

    def generateAddHeaderConstructorArguments(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          "(_value, _requestBuilder.withAddedHeaders(newHeaders: _*))"
        case None => "(_requestBuilder.withAddedHeaders(newHeaders: _*))"
      }

    def generateSetHeaderConstructorArguments(resource: RichResource): String =
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          "(_value, _requestBuilder.withSetHeaders(newHeaders: _*))"
        case None => "(_requestBuilder.withSetHeaders(newHeaders: _*))"
      }

    def generateParameterType(parameterType: ParsedType): String = {
      parameterType match {
        case stringType: ParsedString   => "String"
        case integerType: ParsedInteger => "Long"
        case numberType: ParsedNumber   => "Double"
        case booleanType: ParsedBoolean => "Boolean"
        case x                          => sys.error(s"Unknown URL parameter type $x")
      }
    }

    def generateResourceFieldImports(resource: RichResource, excludePackage: List[String]): Option[String] = {
      if (excludePackage != resource.classRep.packageParts) Some(s"import ${resource.classRep.fullyQualifiedName}")
      else None
    }

    def generateResourceDslField(resource: RichResource): String = {

      import CleanNameUtil._

      val cleanUrlSegment = escapeScalaKeyword(cleanMethodName(resource.urlSegment))
      resource.urlParameter match {
        case Some(parameter) =>
          val paramType = generateParameterType(parameter.parameterType.parsed)
          s"""def $cleanUrlSegment(value: $paramType) = new ${resource.classRep.fullyQualifiedName}(value, _requestBuilder.withAddedPathSegment(value))"""
        case None =>
          s"""def $cleanUrlSegment = new ${resource.classRep.fullyQualifiedName}(_requestBuilder.withAddedPathSegment("${resource.urlSegment}"))"""
      }
    }

    generateClientClass(resources) ::: resources.flatMap(generateResourceClassesHelper)
  }

}
