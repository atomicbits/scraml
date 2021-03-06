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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.atomicbits.scraml.dsl.javajackson.DateTimeOnly;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by peter on 8/10/17.
 */
public class DateTimeOnlyDeserializer extends JsonDeserializer<DateTimeOnly> {

    @Override
    public DateTimeOnly deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        DateTimeOnly dateTimeOnly = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dateTimeOnly = new DateTimeOnly();
            dateTimeOnly.setDateTime(localDateTime);
        }

        return dateTimeOnly;
    }

}
