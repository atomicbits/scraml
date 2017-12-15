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
import io.atomicbits.scraml.dsl.androidjavajackson.DateTimeRFC2616;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by peter on 8/10/17.
 */
public class DateTimeRFC2616Deserializer extends JsonDeserializer<DateTimeRFC2616> {

    @Override
    public DateTimeRFC2616 deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        DateTimeRFC2616 dateTimeRFC2616 = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.getDefault());

            try {
                Date date = format.parse(dateString);
                dateTimeRFC2616 = new DateTimeRFC2616();
                dateTimeRFC2616.setDateTime(date);
            } catch (ParseException e) {
                throw new JsonParseException(
                        "The date " + dateString + " is not a RFC2616 date (EEE, dd MMM yyyy HH:mm:ss 'GMT').",
                        jp.getCurrentLocation(),
                        e
                );
            }

        }

        return dateTimeRFC2616;
    }

}
