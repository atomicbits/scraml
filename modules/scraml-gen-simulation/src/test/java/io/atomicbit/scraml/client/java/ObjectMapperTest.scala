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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.{ObjectWriter, JavaType, ObjectMapper}
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

      val person = new Person()
      person.setFirstName("John")
      person.setLastName("Doe")
      person.setAge(21L)

      val personList: JList[Person] = util.Arrays.asList(person)

      val javaType: JavaType = TypeFactory.defaultInstance.constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Person>")
      val readPerson: JList[Person] = objectMapper.readValue( """[{"firstName":"John", "lastName": "Doe", "age": 21}]""", javaType)

      Then("The correct data type is read")

      println(s"Person list is: $readPerson")
      assertResult(personList)(readPerson) // equals is overriden in Person

      // see if we can overcome type erasure
      // val writer: ObjectWriter = objectMapper.writerFor(javaType)
      val serializedPersonList: String = objectMapper.writeValueAsString(personList) // writer.writeValueAsString(personList)
      assertResult( """[{"firstName":"John","lastName":"Doe","age":21}]""")(serializedPersonList)

    }


    scenario("Test the Object Mapper on a type hierarcy") {
      val objectMapper: ObjectMapper = new ObjectMapper

      val dog: Dog = new Dog(true, "female", "Ziva")

      val serializedDog: String = objectMapper.writeValueAsString(dog)
      assertResult( """{"_type":"Dog","canBark":true,"gender":"female","name":"Ziva"}""")(serializedDog)
    }


    scenario("Test the object mapper on a type hierarcy in the type erased context of Java Lists") {
      val objectMapper: ObjectMapper = new ObjectMapper

      val animals: JList[Animal] = util.Arrays.asList(new Dog(true, "male", "Wiskey"), new Fish("Wanda"), new Cat("male", "Duster"))

      val javaType: JavaType = TypeFactory.defaultInstance.constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Animal>")

      val writer: ObjectWriter = objectMapper.writerFor(javaType)

      val serializedAnimals: String = writer.writeValueAsString(animals) // objectMapper.writeValueAsString(animals)
      println("animals: " + serializedAnimals)
      assertResult( """[{"_type":"Dog","canBark":true,"gender":"male","name":"Wiskey"},{"_type":"Fish","gender":"Wanda"},{"_type":"Cat","gender":"male","name":"Duster"}]""")(serializedAnimals)
    }


  }

}
