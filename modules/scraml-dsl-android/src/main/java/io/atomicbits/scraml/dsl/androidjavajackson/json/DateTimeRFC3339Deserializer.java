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
import io.atomicbits.scraml.dsl.androidjavajackson.DateTimeRFC3339;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by peter on 8/10/17.
 *
 * See: https://www.ietf.org/rfc/rfc3339.txt
 *
 */
public class DateTimeRFC3339Deserializer extends JsonDeserializer<DateTimeRFC3339> {


    @Override
    public DateTimeRFC3339 deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        DateTimeRFC3339 dateTimeRFC3339 = null;
        String dateString = jp.getText();

        if (dateString != null && !dateString.isEmpty()) {

            Date date = null;

            // Keep in mind that SimpleDateFormat is NOT THREADSAFE!
            // Don't instantiate these variables on the class-level! Leave them here!
            List<SimpleDateFormat> knownPatterns = new ArrayList<SimpleDateFormat>();
            // See:
            // https://stackoverflow.com/questions/40369287/what-pattern-should-be-used-to-parse-rfc-3339-datetime-strings-in-java
            // https://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat/4024604#4024604
            // the first three, see: play.api.libs.json.IsoDateReads
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"));
            // the last in the row, see:
            // https://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat/4024604#4024604
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss'Z'"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

            for (SimpleDateFormat pattern : knownPatterns) {
                try {
                    if (date == null) {
                        date = pattern.parse(dateString);
                    }
                } catch (ParseException pe) {
                    // Loop on
                }
            }

            if (date == null) {
                throw new JsonParseException("The date " + dateString + " is not an RFC3339 date.", jp.getCurrentLocation());
            }

            dateTimeRFC3339 = new DateTimeRFC3339();
            dateTimeRFC3339.setDateTime(date);
        }

        return dateTimeRFC3339;
    }

}
