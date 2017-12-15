/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
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
import io.atomicbits.scraml.dsl.androidjavajackson.TimeOnly;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by peter on 8/10/17.
 */
public class TimeOnlyDeserializer extends JsonDeserializer<TimeOnly> {

    @Override
    public TimeOnly deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        TimeOnly timeOnly = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {

            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss[.SSS]", Locale.getDefault());

            try {
                Date date = format.parse(dateString);
                timeOnly = new TimeOnly();
                timeOnly.setTime(date);
            } catch (ParseException e) {
                throw new JsonParseException(
                        "The date " + dateString + " is not a time-only date (HH:mm:ss[.SSS]).",
                        jp.getCurrentLocation(),
                        e
                );
            }

        }

        return timeOnly;
    }

}
