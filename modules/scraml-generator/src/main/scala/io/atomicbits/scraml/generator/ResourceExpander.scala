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
import io.atomicbits.scraml.parser.model._


/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ResourceExpander {


  /**
   * Expanding a resource consists of two high-level steps:
   * 1. expand the current path segment (possibly a path parameter) if it is non-empty and expand it into the DSL
   * 2. expand the resource's actions and sub-resources recursively
   */
  def expandResource(resource: Resource, schemaLookup: SchemaLookup): String = {

    val segmentAsString = resource.urlSegment
    val segmentAsDefName = cleanupUrlSegment(resource.urlSegment)

    val expandedSubResources = resource.resources.map(resource => expandResource(resource, schemaLookup))
    val expandedActions = resource.actions.flatMap(action => ActionExpander.expandAction(action, schemaLookup))

    def noSegment = {
      s"""
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
        """
    }

    def plainSegment = {
      s"""
            def $segmentAsDefName = new PlainSegment("$segmentAsString", requestBuilder) {
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
            }
      """
    }

    def stringSegment = {
      s"""
            def $segmentAsDefName(value: String) = new ParamSegment[String](value, requestBuilder) {
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
            }
      """
    }

    def intSegment = {
      s"""
            def $segmentAsDefName(value: Int) = new ParamSegment[Int](value, requestBuilder) {
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
            }
      """
    }

    def doubleSegment = {
      s"""
            def $segmentAsDefName(value: Double) = new ParamSegment[Double](value, requestBuilder) {
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
            }
      """
    }

    def booleanSegment = {
      s"""
            def $segmentAsDefName(value: Boolean) = new ParamSegment[Boolean](value, requestBuilder) {
              ${expandedActions.mkString("\n")}
              ${expandedSubResources.mkString("\n")}
            }
      """
    }

    if (resource.urlSegment.isEmpty) {
      noSegment
    } else {
      resource.urlParameter match {
        case None                               => plainSegment
        case Some(Parameter(StringType, _, _))  => stringSegment
        case Some(Parameter(IntegerType, _, _)) => intSegment
        case Some(Parameter(NumberType, _, _))  => doubleSegment
        case Some(Parameter(BooleanType, _, _)) => booleanSegment
        case Some(x)                            => sys.error(s"Unknown URL parameter type $x")
      }
    }

  }

  private def cleanupUrlSegment(segment: String): String = {
    // cleanup all strange characters
    segment.trim.replaceAll("[-]", "")
  }

}
