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

package io.atomicbits.scraml.client.java

import java.util
import java.util.{ List => JList }

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.{ JavaType, ObjectMapper, ObjectWriter }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec

/**
  * Created by peter on 12/10/15.
  */
class ObjectMapperTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  Feature("We have an Object Mapper to serialize and deserialize objects") {

    Scenario("Test the Object Mapper on canonical object representations") {

      Given("An object mapper")

      val objectMapper: ObjectMapper = new ObjectMapper

      When("Some data is provided to the object mapper to read")

      val person = new Person()
      person.setFirstName("John")
      person.setLastName("Doe")
      person.setAge(21L)

      val personList: JList[Person] = util.Arrays.asList(person)

      val javaType: JavaType        = TypeFactory.defaultInstance.constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Person>")
      val readPerson: JList[Person] = objectMapper.readValue("""[{"firstName":"John", "lastName": "Doe", "age": 21}]""", javaType)

      Then("The correct data type is read")

      println(s"Person list is: $readPerson")
      assertResult(personList)(readPerson) // equals is overriden in Person

      // see if we can overcome type erasure
      // val writer: ObjectWriter = objectMapper.writerFor(javaType)
      val serializedPersonList: String = objectMapper.writeValueAsString(personList) // writer.writeValueAsString(personList)
      assertResult("""[{"firstName":"John","lastName":"Doe","age":21}]""")(serializedPersonList)

    }

    Scenario("Test the Object Mapper on a type hierarcy") {
      val objectMapper: ObjectMapper = new ObjectMapper

      val dog: Dog = new Dog(true, "female", "Ziva")

      val serializedDog: String = objectMapper.writeValueAsString(dog)
      assertResult("""{"_type":"Dog","canBark":true,"gender":"female","name":"Ziva"}""")(serializedDog)
    }

    Scenario("Test the object mapper on a type hierarcy in the type erased context of Java Lists") {
      val objectMapper: ObjectMapper = new ObjectMapper

      val animals: JList[Animal] = util.Arrays.asList(new Dog(true, "male", "Wiskey"), new Fish("Wanda"), new Cat("male", "Duster"))

      val javaType: JavaType = TypeFactory.defaultInstance.constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Animal>")

      val writer: ObjectWriter = objectMapper.writerFor(javaType)

      val serializedAnimals: String = writer.writeValueAsString(animals) // objectMapper.writeValueAsString(animals)
      println("animals: " + serializedAnimals)
      assertResult(
        """[{"_type":"Dog","canBark":true,"gender":"male","name":"Wiskey"},{"_type":"Fish","gender":"Wanda"},{"_type":"Cat","gender":"male","name":"Duster"}]""")(
        serializedAnimals)
    }

  }

}
