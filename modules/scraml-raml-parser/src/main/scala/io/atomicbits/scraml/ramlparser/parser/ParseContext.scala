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
