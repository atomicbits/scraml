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

import scala.util.control.NonFatal
import scala.util.{Success, Failure, Try}

/**
  * Created by peter on 20/03/16.
  */
object TryUtils {


  def accumulate[T](tries: Seq[Try[T]]): Try[Seq[T]] = {

    val (successes, errors) = tries.partition(_.isSuccess)

    if (errors.isEmpty) {
      Success(successes.map(_.get))
    } else {
      val errorsTyped =
        errors.collect {
          case Failure(exc) => exc
        }
      Failure(errorsTyped.reduce(addExceptions))
    }
  }


  def accumulate[U, T](tryMap: Map[U, Try[T]]): Try[Map[U, T]] = {
    val (keys, values) = tryMap.toSeq.unzip
    accumulate(values) map { successfulValues =>
      keys.zip(successfulValues).toMap
    }
  }


  def accumulate[T](tryOption: Option[Try[T]]): Try[Option[T]] = {
    tryOption match {
      case Some(Failure(ex)) => Failure[Option[T]](ex)
      case Some(Success(t))  => Success(Option(t))
      case None              => Success(None)
    }
  }


  def addExceptions(exc1: Throwable, exc2: Throwable): Throwable = {
    (exc1, exc2) match {
      case (e1: RamlParseException, e2: RamlParseException) => e1 ++ e2
      case (e1, e2: RamlParseException)                     => e1
      case (e1: RamlParseException, e2)                     => e2
      case (e1, NonFatal(e2))                               => e1
      case (NonFatal(e1), e2)                               => e2
      case (e1, e2)                                         => e1
    }
  }

  
  /**
    * withSuccess collects all failure cases in an applicative way according to the 'addExceptions' specs.
    */
  def withSuccess[A, RES](a: Try[A])(fn: A => RES): Try[RES] = {
    a.map(fn)
  }

  def withSuccess[A, B, RES](a: Try[A], b: Try[B])(fn: (A, B) => RES): Try[RES] =
    withSuccessCurried(a, b)(fn.curried)

  private def withSuccessCurried[A, B, RES](a: Try[A], b: Try[B])(fn: A => B => RES): Try[RES] = {
    val tried: Try[(B => RES)] = withSuccess(a)(fn)
    processDelta(b, tried)
  }


  def withSuccess[A, B, C, RES](a: Try[A], b: Try[B], c: Try[C])(fn: (A, B, C) => RES): Try[RES] =
    withSuccessCurried(a, b, c)(fn.curried)

  private def withSuccessCurried[A, B, C, RES](a: Try[A], b: Try[B], c: Try[C])(fn: A => B => C => RES): Try[RES] = {
    val tried: Try[(C => RES)] = withSuccessCurried(a, b)(fn)
    processDelta(c, tried)
  }


  def withSuccess[A, B, C, D, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D])(fn: (A, B, C, D) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d)(fn.curried)

  private def withSuccessCurried[A, B, C, D, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D])(fn: A => B => C => D => RES): Try[RES] = {
    val tried: Try[(D => RES)] = withSuccessCurried(a, b, c)(fn)
    processDelta(d, tried)
  }


  def withSuccess[A, B, C, D, E, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E])(fn: (A, B, C, D, E) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E])
                                                    (fn: A => B => C => D => E => RES): Try[RES] = {
    val tried: Try[(E => RES)] = withSuccessCurried(a, b, c, d)(fn)
    processDelta(e, tried)
  }


  def withSuccess[A, B, C, D, E, F, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F])
                                        (fn: (A, B, C, D, E, F) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F])
                                                       (fn: A => B => C => D => E => F => RES): Try[RES] = {
    val tried: Try[(F => RES)] = withSuccessCurried(a, b, c, d, e)(fn)
    processDelta(f, tried)
  }


  def withSuccess[A, B, C, D, E, F, G, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G])
                                           (fn: (A, B, C, D, E, F, G) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f, g)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, G, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G])
                                                          (fn: A => B => C => D => E => F => G => RES): Try[RES] = {
    val tried: Try[(G => RES)] = withSuccessCurried(a, b, c, d, e, f)(fn)
    processDelta(g, tried)
  }


  def withSuccess[A, B, C, D, E, F, G, H, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G], h: Try[H])
                                              (fn: (A, B, C, D, E, F, G, H) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f, g, h)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, G, H, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G],
                                                              h: Try[H])
                                                             (fn: A => B => C => D => E => F => G => H => RES): Try[RES] = {
    val tried: Try[(H => RES)] = withSuccessCurried(a, b, c, d, e, f, g)(fn)
    processDelta(h, tried)
  }


  def withSuccess[A, B, C, D, E, F, G, H, I, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G], h: Try[H],
                                                  i: Try[I])
                                                 (fn: (A, B, C, D, E, F, G, H, I) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f, g, h, i)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, G, H, I, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F],
                                                                 g: Try[G], h: Try[H], i: Try[I])
                                                                (fn: A => B => C => D => E => F => G => H => I => RES): Try[RES] = {
    val tried: Try[(I => RES)] = withSuccessCurried(a, b, c, d, e, f, g, h)(fn)
    processDelta(i, tried)
  }


  def withSuccess[A, B, C, D, E, F, G, H, I, J, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G],
                                                     h: Try[H], i: Try[I], j: Try[J])
                                                    (fn: (A, B, C, D, E, F, G, H, I, J) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f, g, h, i, j)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, G, H, I, J, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F],
                                                                    g: Try[G], h: Try[H], i: Try[I], j: Try[J])
                                                                   (fn: A => B => C => D => E => F => G => H => I => J => RES): Try[RES] = {
    val tried: Try[(J => RES)] = withSuccessCurried(a, b, c, d, e, f, g, h, i)(fn)
    processDelta(j, tried)
  }


  def withSuccess[A, B, C, D, E, F, G, H, I, J, K, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F], g: Try[G],
                                                        h: Try[H], i: Try[I], j: Try[J], k: Try[K])
                                                       (fn: (A, B, C, D, E, F, G, H, I, J, K) => RES): Try[RES] =
    withSuccessCurried(a, b, c, d, e, f, g, h, i, j, k)(fn.curried)

  private def withSuccessCurried[A, B, C, D, E, F, G, H, I, J, K, RES](a: Try[A], b: Try[B], c: Try[C], d: Try[D], e: Try[E], f: Try[F],
                                                                       g: Try[G], h: Try[H], i: Try[I], j: Try[J], k: Try[K])
                                                                      (fn: A => B => C => D => E => F => G => H => I => J =>
                                                                        K => RES): Try[RES] = {
    val tried: Try[(K => RES)] = withSuccessCurried(a, b, c, d, e, f, g, h, i, j)(fn)
    processDelta(k, tried)
  }


  private def processDelta[X, RES](delta: Try[X], fn: Try[(X => RES)]): Try[RES] = {
    delta match {
      case Success(value) =>
        fn match {
          case Success(func)     => Success(func(value))
          case Failure(failures) => Failure(failures)
        }
      case Failure(exc)   =>
        fn match {
          case Success(func)     => Failure(exc)
          case Failure(failures) => Failure(addExceptions(failures, exc))
        }
    }
  }

}
