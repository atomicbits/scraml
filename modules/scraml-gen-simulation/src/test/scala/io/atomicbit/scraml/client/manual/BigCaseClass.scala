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

package io.atomicbit.scraml.client.manual

import play.api.libs.json._

/**
  * Created by peter on 26/09/16.
  */
case class BigCaseClass(a: Option[String],
                        b: String,
                        c: String,
                        d: String,
                        e: String,
                        f: String,
                        g: String,
                        h: String,
                        i: String,
                        j: String,
                        k: String,
                        l: String,
                        m: String,
                        n: String,
                        o: String,
                        p: String,
                        q: String,
                        r: String,
                        s: String,
                        t: String,
                        u: String,
                        v: String,
                        w: String,
                        x: String,
                        y: String,
                        z: String)

object BigCaseClass {

  import play.api.libs.functional.syntax._

  implicit def jsonFormatter: Format[BigCaseClass] = {

    val fields1 =
      ((__ \ "a").formatNullable[String] ~
        (__ \ "b").format[String] ~
        (__ \ "c").format[String] ~
        (__ \ "d").format[String] ~
        (__ \ "e").format[String] ~
        (__ \ "f").format[String] ~
        (__ \ "g").format[String] ~
        (__ \ "h").format[String] ~
        (__ \ "i").format[String] ~
        (__ \ "j").format[String] ~
        (__ \ "k").format[String] ~
        (__ \ "l").format[String] ~
        (__ \ "m").format[String] ~
        (__ \ "n").format[String] ~
        (__ \ "o").format[String] ~
        (__ \ "p").format[String] ~
        (__ \ "q").format[String] ~
        (__ \ "r").format[String] ~
        (__ \ "s").format[String] ~
        (__ \ "t").format[String] ~
        (__ \ "u").format[String]).tupled

    val fields2 =
      ((__ \ "v").format[String] ~
        (__ \ "w").format[String] ~
        (__ \ "x").format[String] ~
        (__ \ "y").format[String] ~
        (__ \ "z").format[String]).tupled

    (fields1 and fields2).apply(
      {
        case ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u), (v, w, x, y, z)) =>
          BigCaseClass.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z)
      },
      bcc =>
        ((bcc.a,
          bcc.b,
          bcc.c,
          bcc.d,
          bcc.e,
          bcc.f,
          bcc.g,
          bcc.h,
          bcc.i,
          bcc.j,
          bcc.k,
          bcc.l,
          bcc.m,
          bcc.n,
          bcc.o,
          bcc.p,
          bcc.q,
          bcc.r,
          bcc.s,
          bcc.t,
          bcc.u),
         (bcc.v, bcc.w, bcc.x, bcc.y, bcc.z))
    )
  }

}
