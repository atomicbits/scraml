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

package io.atomicbits.scraml.dsl.scalaplay.json

import play.api.libs.json._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.language.experimental.macros

/**
  * Created by Renato Cavalcanti and Christophe Vanfleteren.
  */
object TypedJson {

  implicit class FormatsOpts[T](val format: Format[T]) extends AnyVal {

    /**
      * Creates a TypeHintFormat that will use the classname of the type T as the typeHint of the TypeHintFormat
      */
    def withTypeHint(implicit typeTag: TypeTag[T], classTag: ClassTag[T]): TypeHint[T] = {
      TypeHint(format)
    }

    /**
      * Creates a TypeHintFormat that will use the passed value as the type hint of the TypeHintFormat
      */
    def withTypeHint(typeHint: String)(implicit classTag: ClassTag[T]): TypeHint[T] = {
      TypeHint(format = format, typeHint = typeHint)
    }

  }

  /**
    * A decorator for a regular Format that can add a type hint to the serialised json.
    * That same type hint is then used again when deserialising.
    */
  case class TypeHint[T](typeHint: String, format: Format[T])(implicit classTag: ClassTag[T]) {

    def canWrite(obj: Any): Boolean = {
      obj.getClass.isAssignableFrom(classTag.runtimeClass)
    }

    def uncheckedWrites(typeHintKey: String, obj: Any) = writes(typeHintKey, obj.asInstanceOf[T])

    def writes(typeHintKey: String, obj: T): JsValue = {
      val jsValue = format.writes(obj)

      jsValue match {
        // toevoegen van type informatie
        case jsObject: JsObject => Json.obj(typeHintKey -> typeHint) ++ jsObject
        // het heeft geen zin om een type discriminator toe te voegen op een 'primitive' JsValue
        case js => js
      }
    }

    def reads(json: JsValue): JsResult[T] = format.reads(json)

  }

  object TypeHint {

    /**
      * Create a new TypeHintFormat, using the classname as the tagValue.
      */
    def apply[T](format: Format[T])(implicit typeTag: TypeTag[T], classTag: ClassTag[T]): TypeHint[T] = {
      new TypeHint[T](typeHint = typeOf[T].toString, format = format)(classTag)
    }

  }

  /**
    * Create a Format that will find the correct format amongst the passed formats when serialising/deserialising,
    * using the type hint in the TypeFormat.
    * The field that holds the type hint will be the value of `typeKey`
    */
  case class TypeHintFormat[A](typeHintKey: String, typeHintFormats: TypeHint[_ <: A]*) extends Format[A] {

    require(typeHintFormats.map(_.typeHint).toSet.size == typeHintFormats.size, "Duplicate type hints in the passed typeHintFormats")

    override def writes(obj: A): JsValue = {
      val formatOpt = typeHintFormats.find(_.canWrite(obj))

      formatOpt match {
        case Some(typedFormat) => typedFormat.uncheckedWrites(typeHintKey, obj)
        case None =>
          sys.error(
            s"""
             |No json format found for class of runtime type ${obj.getClass}
              |There where TypeHintFormats defined for the following typeValues: ${typeHintFormats.map(_.typeHint)}
              |Did you pass a TypeHintFormat for the type ${obj.getClass} to the TaggedFormats?""".stripMargin
          )
      }
    }

    def reads(json: JsValue): JsResult[A] = {

      val typeHintOpt = (json \ typeHintKey).asOpt[String]
      typeHintOpt match {
        case None =>
          JsError(
            s"Expected a field named $typeHintKey in the json to use as typeHint. Now I do not now what Format to use to read the json."
          )
        case Some(typeHint) =>
          val formatOpt = typeHintFormats.find(_.typeHint == typeHint)

          formatOpt match {
            case Some(typedFormat) => typedFormat.reads(json)
            case None =>
              JsError(
                s"""
                 |No json format found for the json with typeHint $typeHint
                  |There where TypeHintFormats defined for the following typeHints: ${typeHintFormats.map(_.typeHint)}
                  |Did you pass a TypeHintFormat for the type $typeHint to the TaggedFormats?""".stripMargin
              )
          }
      }
    }

    def ++[U >: A, B <: U](that: TypeHintFormat[B]): TypeHintFormat[U] = {

      require(this.typeHintKey == that.typeHintKey, "Merged TypeHintFormats must have the same typeHintKey")

      val aHints = this.typeHintFormats.asInstanceOf[Seq[TypeHint[U]]]
      val bHints = that.typeHintFormats.asInstanceOf[Seq[TypeHint[U]]]

      val mergedHints = aHints ++ bHints

      TypeHintFormat[U](this.typeHintKey, mergedHints: _*)
    }

  }

  object TypeHintFormat {

    /**
      * Create a Format that will find the correct format amongst the passed formats when serialising/deserialising,
      * using the type hint in the TypeFormat.
      * The field that holds the type hint will be "_type"
      */
    def apply[A](typedFormats: TypeHint[_ <: A]*): TypeHintFormat[A] = {
      TypeHintFormat[A](typeHintKey = "_type", typedFormats: _*)
    }

  }

}
