/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Raml(resources: List[Resource], schemas: Map[String, String])

object Raml {


  def apply(raml: org.raml.model.Raml): Raml = {

    val parallelResources: List[Resource] = raml.getResources.values().asScala.toList.map(Resource(_))

    // Now, we can still have parallel resources that have overlapping paths because they could result
    // from inclusions in a main raml definition file. We have to merge those parallel resource paths first at all depths.
    val resources: List[Resource] = unparallellizeResources(parallelResources)

    val linkedSchemas: Map[String, String] = {
      Option(raml.getSchemas) map { schema =>
        schema.asScala.foldLeft[Map[String, String]](Map.empty)((aggr, el) => aggr ++ el.asScala)
      } getOrElse Map.empty
    }

    val (resourcesWithoutInlineSchemas, updatedLinkedSchemas) =
      resources.foldLeft[SchemaResourceExtraction]((List.empty, linkedSchemas))(extractFromResource)

    Raml(resourcesWithoutInlineSchemas, updatedLinkedSchemas)
  }


  type SchemaResourceExtraction = (List[Resource], Map[String, String])
  type SchemaActionExtraction = (List[Action], Map[String, String])
  type SchemaResponseExtraction = (Map[String, Response], Map[String, String])
  type Body = Map[String, MimeType]
  type SchemaBodyExtraction = (Body, Map[String, String])

  /**
   * Inline schema's are located inside the MimeType of Action body and Response body elements.
   * We will replace them with a generated link "_inline{number}" and add the link to the linked schemas.
   *
   * @param schemaExtraction The resources that are already updated with the linked schemas.
   * @param resource The resource to extract inline schemas from.
   * @return The updated version of the schema extraction.
   */
  private def extractFromResource(schemaExtraction: SchemaResourceExtraction,
                                  resource: Resource): SchemaResourceExtraction = {

    val (processedResources, linkedSchemas) = schemaExtraction

    val (actionsWithoutInlineSchemas, updatedLinkedSchemasFromActions) =
      resource.actions.foldLeft[SchemaActionExtraction](List.empty, linkedSchemas)(extractFromAction)

    val (childResourcesWithoutInlineSchemas, updatedLinkedSchemas) =
      resource.resources
        .foldLeft[SchemaResourceExtraction]((List.empty, updatedLinkedSchemasFromActions))(extractFromResource)

    val updatedResource =
      resource.copy(
        actions = actionsWithoutInlineSchemas,
        resources = childResourcesWithoutInlineSchemas
      )

    (updatedResource :: processedResources, updatedLinkedSchemas)

  }


  private def extractFromAction(schemaExtraction: SchemaActionExtraction, action: Action): SchemaActionExtraction = {

    val (processedActions, linkedSchemas) = schemaExtraction

    val (updatedBody, updatedLinkedSchemasFromBody) =
      action.body.toList.foldLeft[SchemaBodyExtraction]((Map.empty, linkedSchemas))(extractFromBodyPart)

    val (responsesWithoutInlineSchemas, updatedLinkedSchemasFromResponses) =
      action.responses.toList
        .foldLeft[SchemaResponseExtraction]((Map.empty, updatedLinkedSchemasFromBody))(extractFromResponse)

    val updatedAction = action.copy(body = updatedBody, responses = responsesWithoutInlineSchemas)

    (updatedAction :: processedActions, updatedLinkedSchemasFromResponses)

  }

  private def extractFromResponse(schemaExtraction: SchemaResponseExtraction,
                                  mimeResponse: (String, Response)): SchemaResponseExtraction = {

    val (processedMimeResponses, linkedSchemas) = schemaExtraction

    val (mimeType, response) = mimeResponse

    val (updatedBody, updatedLinkedSchemasFromBody) =
      response.body.toList.foldLeft[SchemaBodyExtraction]((Map.empty, linkedSchemas))(extractFromBodyPart)

    val updatedResponse = response.copy(body = updatedBody)

    (processedMimeResponses + (mimeType -> updatedResponse), updatedLinkedSchemasFromBody)

  }


  private def extractFromBodyPart(schemaExtraction: SchemaBodyExtraction,
                                  bodyPart: (String, MimeType)): SchemaBodyExtraction = {

    val (updatedBody, linkedSchemas) = schemaExtraction

    val (mime, mimeType) = bodyPart

    type SchemaLinkUpdate = (Option[String], Map[String, String])

    var (updatedSchemaLinkOpt, updatedLinkedSchemas) =
      mimeType.schema.foldLeft[SchemaLinkUpdate]((None, linkedSchemas)) { (schemaExtraction, explicietOrInlineSchema) =>
        val (replacementString, linkedSchemas) = schemaExtraction
        if (linkedSchemas.get(explicietOrInlineSchema).isEmpty) {
          // we have an inline schema
          val link = s"_inline-${linkedSchemas.keys.size}"
          (Some(link), linkedSchemas + (link -> explicietOrInlineSchema))
        } else {
          (None, linkedSchemas)
        }
      }

    updatedSchemaLinkOpt match {
      case linkOpt@Some(link) =>
        val updatedMImeType = mimeType.copy(schema = linkOpt)
        (updatedBody + (mime -> updatedMImeType), updatedLinkedSchemas)
      case None               => (updatedBody + bodyPart, linkedSchemas)
    }

  }


  /**
   * Unparallellize the given resources at all levels. This must be done top-down!
   */
  private def unparallellizeResources(resources: List[Resource]): List[Resource] = {

    // Group all resources at this level with the same urlSegment and urlParameter
    val groupedResources: List[List[Resource]] =
      resources.groupBy(resource => (resource.urlSegment, resource.urlParameter)).values.toList

    // Merge all actions and subresources of all resources that have the same (urlSegment, urlParameter)
    def mergeResources(resources: List[Resource]): Resource = {
      resources.reduce { (resourceA, resourceB) =>
        resourceA.copy(actions = resourceA.actions ++ resourceB.actions, resources = resourceA.resources ++ resourceB.resources)
      }
    }
    val mergedResources: List[Resource] = groupedResources.map(mergeResources)

    mergedResources.map { mergedResource =>
      mergedResource.copy(resources = unparallellizeResources(mergedResource.resources))
    }

  }


}
