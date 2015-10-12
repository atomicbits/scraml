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

package io.atomicbits.scraml.client.java

import java.util

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.{JavaType, ObjectMapper}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, FeatureSpec}
import java.util.{List => JList}

/**
 * Created by peter on 12/10/15.
 */
class ObjectMapperTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {


  feature("We have an Object Mapper to serialize and deserialize objects") {

    scenario("Test the Object Mapper on canonical object representations") {


      Given("An object mapper")

      val objectMapper: ObjectMapper = new ObjectMapper


      When("Some data is provided to the object mapper to read")

      val person = new Persoon()
      person.setFirstName("John")
      person.setLastName("Doe")
      person.setAge(21L)

      val personList: JList[Persoon] = util.Arrays.asList(person)

      val javaType: JavaType = TypeFactory.defaultInstance.constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Persoon>")
      val readPerson: JList[Persoon] = objectMapper.readValue("""[{"firstName":"John", "lastName": "Doe", "age": 21}]""", javaType)


      Then("The correct data type is read")

      println(s"Persoon list is: $readPerson")
      assertResult(personList)(readPerson)

    }
  }

}
