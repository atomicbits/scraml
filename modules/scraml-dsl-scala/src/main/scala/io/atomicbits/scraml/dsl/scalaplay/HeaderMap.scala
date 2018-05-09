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

package io.atomicbits.scraml.dsl.scalaplay

import _root_.java.util.Locale

/**
  * Created by peter on 30/10/15.
  */
case class HeaderMap(private val headerList: Map[String, List[String]] = Map.empty,
                     private val originalKeys: Map[String, String]     = Map.empty) {

  /**
    * + will add given headers and expand existing headers with additional values
    */
  def +(keyValuePair: (String, String)): HeaderMap = {
    val (key, value) = keyValuePair

    val keyNormalized: String = normalize(key)
    val valueOriginal: String = value.trim

    if (keyNormalized.isEmpty || valueOriginal.isEmpty) {
      this
    } else {
      val updatedOriginalKeys = originalKeys + (keyNormalized -> key)
      val valueList =
        headerList.get(keyNormalized) map { currentValues =>
          valueOriginal :: currentValues
        } getOrElse List(valueOriginal)
      val updatedHeaders = headerList + (keyNormalized -> valueList)
      this.copy(headerList = updatedHeaders, originalKeys = updatedOriginalKeys)
    }
  }

  /**
    * + will add given headers and expand existing headers with additional values
    */
  def ++(keyValuePairs: (String, String)*): HeaderMap =
    keyValuePairs.foldLeft(this) { (headerMap, keyValuePair) =>
      headerMap + keyValuePair
    }

  /**
    * + will add given headers and expand existing headers with additional values
    */
  def ++(headerMap: HeaderMap): HeaderMap =
    headerMap.headers.foldLeft(this) { (headMap, header) =>
      val (key, values) = header
      values.foldLeft(headMap) { (hMap, value) =>
        hMap + (key -> value)
      }
    }

  /**
    * set wil overwrite existing headers rather than extend them with an additional value
    */
  def set(keyValuePair: (String, String)): HeaderMap = {
    val (key, value) = keyValuePair
    setMany((key, List(value)))
  }

  /**
    * set wil overwrite existing headers rather than extend them with an additional value
    */
  def setMany(keyValuePair: (String, List[String])): HeaderMap = {
    val (key, values) = keyValuePair

    val keyNormalized: String        = normalize(key)
    val valuesOriginal: List[String] = values.map(_.trim)

    if (keyNormalized.isEmpty || valuesOriginal.isEmpty) {
      this
    } else {
      val updatedOriginalKeys = originalKeys + (keyNormalized -> key)
      val updatedHeaders      = headerList + (keyNormalized -> valuesOriginal)
      this.copy(headerList = updatedHeaders, originalKeys = updatedOriginalKeys)
    }
  }

  /**
    * set wil overwrite existing headers rather than extend them with an additional value
    */
  def set(keyValuePairs: (String, String)*): HeaderMap =
    keyValuePairs.foldLeft(this) { (headerMap, keyValuePair) =>
      headerMap set keyValuePair
    }

  /**
    * set wil overwrite existing headers rather than extend them with an additional value
    */
  def set(headerMap: HeaderMap): HeaderMap = {
    headerMap.headers.foldLeft(this) { (headMap, header) =>
      headMap setMany header
    }
  }

  def headers: Map[String, List[String]] = {
    originalKeys.keys.foldLeft(Map.empty[String, List[String]]) { (map, normalizedKey) =>
      map + (originalKeys(normalizedKey) -> headerList(normalizedKey))
    }
  }

  def hasKey(key: String): Boolean = {
    originalKeys.get(normalize(key)).isDefined
  }

  def get(key: String): Option[List[String]] = {
    headerList.get(normalize(key))
  }

  def foreach(f: ((String, List[String])) => Unit) = {
    originalKeys.keys foreach { normalizedKey =>
      f(originalKeys(normalizedKey), headerList(normalizedKey))
    }
  }

  private def normalize(key: String): String = {
    key.trim.toLowerCase(Locale.ENGLISH)
  }

}
