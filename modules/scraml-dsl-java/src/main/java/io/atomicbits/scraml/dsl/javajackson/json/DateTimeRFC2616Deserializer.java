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
import io.atomicbits.scraml.dsl.javajackson.DateTimeRFC2616;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by peter on 8/10/17.
 */
public class DateTimeRFC2616Deserializer extends JsonDeserializer<DateTimeRFC2616> {

    @Override
    public DateTimeRFC2616 deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        DateTimeRFC2616 dateTimeRFC2616 = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME);
            dateTimeRFC2616 = new DateTimeRFC2616();
            dateTimeRFC2616.setDateTime(offsetDateTime);
        }

        return dateTimeRFC2616;
    }

}
