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

package io.atomicbits.scraml.generator.typemodel

import io.atomicbits.scraml.ramlparser.model.Resource

/**
  * Created by peter on 13/01/17.
  *
  * In a resource class definition, we collect all information that is needed to generate a single resource class, independent from
  * the target language.
  */
case class ResourceClassDefinition(basePackage: List[String],
                                   precedingUrlSegments: List[String],
                                   resource: Resource,
                                   childResources: List[Resource])
    extends SourceDefinition
