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

package io.atomicbits.scraml.client.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by peter on 16/10/15.
 */

/**
 * Beware that this is a Java test and as such, it will not be executed by default by <sbt test> !
 * There is an exact copy-scala implementation called 'ObjectMapperTest' that does the same tests. This class is kept for nostalgic reasons.
 */
public class ObjectMapperJavaTest {

    @Test
    public void testObjectMapperWithCanonicalRepresentation() {

        ObjectMapper objectMapper = new ObjectMapper();

        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setAge(21L);

        List<Person> personList = Arrays.asList(person);

        JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Person>");

        try {
            List<Person> readPerson = objectMapper.readValue("[{\"firstName\":\"John\", \"lastName\": \"Doe\", \"age\": 21}]", javaType);


            assertEquals(personList, readPerson);

            String serializedPersonList = objectMapper.writeValueAsString(personList); // writer.writeValueAsString(personList)
            assertEquals("[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":21}]", serializedPersonList);

        } catch (IOException e) {
            fail("Did not expect exception: " + e.getMessage());
        }
    }


    @Test
    public void testObjectMapperOnHierarcy() {

        ObjectMapper objectMapper = new ObjectMapper();

        Dog dog = new Dog(true, "female", "Ziva");

        try {
            String serializedDog = objectMapper.writeValueAsString(dog);
            // System.out.println("dog: " + serializedDog);
            assertEquals("{\"_type\":\"Dog\",\"canBark\":true,\"gender\":\"female\",\"name\":\"Ziva\"}", serializedDog);
        } catch (JsonProcessingException e) {
            fail("Did not expect exception: " + e.getMessage());
        }
    }


    @Test
    public void testObjectMapperOnHierarcyWithTypeErasure() {

        ObjectMapper objectMapper = new ObjectMapper();

        List<Animal> animals = Arrays.asList(
                new Dog(true, "male", "Wiskey"),
                new Fish("Wanda"),
                new Cat("male", "Duster")
        );

        JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical("java.util.List<io.atomicbits.scraml.client.java.Animal>");

        ObjectWriter writer = objectMapper.writerFor(javaType);

        try {
            String serializedAnimals = writer.writeValueAsString(animals);
            System.out.println("dog: " + serializedAnimals);
            assertEquals("[{\"_type\":\"Dog\",\"canBark\":true,\"gender\":\"male\",\"name\":\"Wiskey\"}," +
                            "{\"_type\":\"Fish\",\"gender\":\"Wanda\"}," +
                            "{\"_type\":\"Cat\",\"gender\":\"male\",\"name\":\"Duster\"}]",
                    serializedAnimals);
        } catch (JsonProcessingException e) {
            fail("Did not expect exception: " + e.getMessage());
        }
    }

}
