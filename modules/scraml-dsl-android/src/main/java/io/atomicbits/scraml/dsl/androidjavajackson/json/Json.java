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

package io.atomicbits.scraml.dsl.androidjavajackson.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.atomicbits.scraml.dsl.androidjavajackson.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 28/03/17.
 */
public class Json {

    /**
     * Reuse of ObjectMapper and JsonFactory is very easy: they are thread-safe provided that configuration is done before any use
     * (and from a single thread). After initial configuration use is fully thread-safe and does not need to be explicitly synchronized.
     * Source: http://wiki.fasterxml.com/JacksonBestPracticesPerformance
     */
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule("WdbModule", new Version(1, 0, 0, null, "io.atomicbits", "scraml"));

        module.addSerializer(DateTimeRFC3339.class, new DateTimeRFC3339Serializer());
        module.addDeserializer(DateTimeRFC3339.class, new DateTimeRFC3339Deserializer());

        module.addSerializer(DateTimeRFC2616.class, new DateTimeRFC2616Serializer());
        module.addDeserializer(DateTimeRFC2616.class, new DateTimeRFC2616Deserializer());

        module.addSerializer(DateTimeOnly.class, new DateTimeOnlySerializer());
        module.addDeserializer(DateTimeOnly.class, new DateTimeOnlyDeserializer());

        module.addSerializer(DateOnly.class, new DateOnlySerializer());
        module.addDeserializer(DateOnly.class, new DateOnlyDeserializer());

        module.addSerializer(TimeOnly.class, new TimeOnlySerializer());
        module.addDeserializer(TimeOnly.class, new TimeOnlyDeserializer());

        objectMapper.registerModule(module);
    }

    /**
     * Write the body to a JSON string.
     * <p>
     * The main reason why we need the canonical form of the request type to serialize the body is in cases where
     * Java type erasure hides access to the Json annotations of our transfer objects.
     * <p>
     * Examples of such type erasure are cases where types in a hierarchy are put inside a {@code java.util.List<B>}. Then, the type of
     * {@code <B>} is hidden in {@code java.util.List<?>}, which hides the @JsonTypeInfo annotations for the objectmapper so that all type info
     * disappears form the resulting JSON objects.
     *
     * @param body                 The actual body.
     * @param canonicalRequestType The canonical form of the request body.
     * @param <B>                  The type of the body.
     * @return The JSON representation of the body as a string.
     */
    public static <B> String writeBodyToString(B body, String canonicalRequestType) {
        if (canonicalRequestType != null && !body.getClass().isEnum() && !body.getClass().isPrimitive()) {
            JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalRequestType);
            ObjectWriter writer = objectMapper.writerFor(javaType);
            try {
                return writer.writeValueAsString(body);
            } catch (IOException e) {
                throw new RuntimeException("JSON serialization error: " + e.getMessage(), e);
            }
        } else {
            return body.toString();
        }
    }

    public static <B> Map<String, HttpParam> toFormUrlEncoded(B body) {
        try {
            JsonNode jsonNode = objectMapper.valueToTree(body);
            Map<String, HttpParam> entries = new HashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (!(entry.getValue() instanceof NullNode))
                    entries.put(
                            entry.getKey(),
                            new SimpleHttpParam(entry.getValue().asText())
                    ); // .asText() is essential here to avoid quoted strings
            }
            return entries;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }

    public static <R> R parseBodyToObject(String body, String canonicalResponseType) {
        JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalResponseType);
        try {
            return objectMapper.readValue(body, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

}
