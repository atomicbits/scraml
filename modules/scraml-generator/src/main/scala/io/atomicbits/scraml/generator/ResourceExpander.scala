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

import scala.reflect.macros.whitebox
import scala.language.experimental.macros


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

    import c.universe._

    val urlSegment = resource.urlSegment
    val segmentAsString = q""" $urlSegment """
    val segmentAsDefName = TermName(cleanupUrlSegment(urlSegment))

    val expandedSubResources = resource.resources.map(resource => expandResource(resource, schemaLookup, c))
    val expandedActions = resource.actions.flatMap(action => ActionExpander.expandAction(action, schemaLookup, c))

    def noSegment = {
      q"""
              ..$expandedActions
              ..$expandedSubResources
           """
    }

    def plainSegment = {
      q"""
            def $segmentAsDefName = new PlainSegment($segmentAsString, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
    }

    def stringSegment = {
      q"""
            def $segmentAsDefName(value: String) = new ParamSegment[String](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
    }

    def intSegment = {
      q"""
            def $segmentAsDefName(value: Int) = new ParamSegment[Int](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
    }

    def doubleSegment = {
      q"""
            def $segmentAsDefName(value: Double) = new ParamSegment[Double](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
    }

    def booleanSegment = {
      q"""
            def $segmentAsDefName(value: Boolean) = new ParamSegment[Boolean](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
    }

    if (resource.urlSegment.isEmpty) {
      noSegment
    } else
      resource.urlParameter match {
        case None => plainSegment
        case Some(Parameter(StringType, _, _)) => stringSegment
        case Some(Parameter(IntegerType, _, _)) => intSegment
        case Some(Parameter(NumberType, _, _)) => doubleSegment
        case Some(Parameter(BooleanType, _, _)) => booleanSegment
        case Some(x) => sys.error(s"Unknown URL parameter type $x")
      }

  }

  private def cleanupUrlSegment(segment: String): String = {
    // cleanup all strange characters
    segment.trim.replaceAll("[-]", "")
  }

}
