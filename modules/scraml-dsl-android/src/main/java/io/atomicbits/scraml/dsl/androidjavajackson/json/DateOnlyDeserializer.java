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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.atomicbits.scraml.dsl.androidjavajackson.DateOnly;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by peter on 8/10/17.
 */
public class DateOnlyDeserializer extends JsonDeserializer<DateOnly> {

    @Override
    public DateOnly deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        DateOnly dateOnly = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            try {
                Date date = format.parse(dateString);
                dateOnly = new DateOnly();
                dateOnly.setDate(date);
            } catch (ParseException e) {
                throw new JsonParseException("The date " + dateString + " is not a date only (yyy-MM-dd).", jp.getCurrentLocation(), e);
            }

        }

        return dateOnly;
    }

}
