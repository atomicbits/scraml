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
import io.atomicbits.scraml.generator.model.RichResource
import io.atomicbits.scraml.parser.model.Resource

/**
 * Created by peter on 22/08/15. 
 */
object ResourceClassGenerator {

  def generateResourceClasses(resources: List[RichResource], schemaLookup: SchemaLookup): List[ClassRep] = {

    // A resource class needs to have one field path entry for each of its child resources. It needs to include the child resource's
    // (fully qualified) class. The resource's package needs to follow the (cleaned) rest path name to guarantee unique class names.
    // A resource class needs to have entries for each of its actions and including all the classes involved in that action definition.


    def generateResourceClass(resource: RichResource): ClassRep = {
      ???
    }


  }




}
