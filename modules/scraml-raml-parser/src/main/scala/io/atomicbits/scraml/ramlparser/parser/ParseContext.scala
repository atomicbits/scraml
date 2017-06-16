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

package io.atomicbits.scraml.ramlparser.parser

import io.atomicbits.scraml.ramlparser.model.{ MediaType, ResourceTypes, Traits }
import play.api.libs.json.{ JsString, JsValue }

/**
  * Created by peter on 10/02/16.
  */
case class ParseContext(var sourceTrail: List[String],
                        var urlSegments: List[String],
                        resourceTypes: ResourceTypes        = ResourceTypes(),
                        traits: Traits                      = Traits(),
                        defaultMediaType: Option[MediaType] = None) {

  def withSourceAndUrlSegments[T](jsValue: JsValue, urlSegs: List[String] = List.empty)(fn: => T): T = {
    val sourceTrailOrig = sourceTrail
    val urlSegmentsOrig = urlSegments

    sourceTrail =
      (jsValue \ Sourced.sourcefield).toOption.collect {
        case JsString(source) => source :: sourceTrailOrig
      } getOrElse {
        sourceTrailOrig
      }
    urlSegments = urlSegmentsOrig ++ urlSegs

    val result = fn

    sourceTrail = sourceTrailOrig
    urlSegments = urlSegmentsOrig

    result
  }

  def head: String = sourceTrail.head

  def resourcePath: String = {
    val resourcePathOptExt = urlSegments.mkString("/")
    if (resourcePathOptExt.endsWith("{ext}")) resourcePathOptExt.dropRight(5)
    else resourcePathOptExt
  }

  def resourcePathName: String = {

    def stripParamSegments(reverseUrlSegs: List[String]): String =
      reverseUrlSegs match {
        case urlSeg :: urlSegs if urlSeg.contains('{') && urlSeg.contains('}') => stripParamSegments(urlSegs)
        case urlSeg :: urlSegs                                                 => urlSeg
        case Nil                                                               => ""
      }

    stripParamSegments(urlSegments.reverse)
  }

}
