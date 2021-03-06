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

package io.atomicbits.scraml.dsl.javajackson.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.atomicbits.scraml.dsl.javajackson.DateOnly;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Created by peter on 8/10/17.
 */
public class DateOnlySerializer extends JsonSerializer<DateOnly> {

    @Override
    public void serialize(DateOnly value, JsonGenerator jgen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        if (value == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(DateTimeFormatter.ISO_LOCAL_DATE.format(value.getDate()));
        }
    }
}
