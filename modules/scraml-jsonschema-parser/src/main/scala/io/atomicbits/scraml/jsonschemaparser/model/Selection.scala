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

package io.atomicbits.scraml.jsonschemaparser.model

/**
 * Created by peter on 7/06/15. 
 */
sealed trait Selection {

  def selection: List[Schema]

  def map(f: Schema => Schema): Selection

}

case class OneOf(selection: List[Schema]) extends Selection {

  override def map(f: (Schema) => Schema): Selection = copy(selection = selection.map(f))
}

case class AnyOf(selection: List[Schema]) extends Selection {

  override def map(f: (Schema) => Schema): Selection = copy(selection = selection.map(f))

}

case class AllOf(selection: List[Schema]) extends Selection {

  override def map(f: (Schema) => Schema): Selection = copy(selection = selection.map(f))

}
