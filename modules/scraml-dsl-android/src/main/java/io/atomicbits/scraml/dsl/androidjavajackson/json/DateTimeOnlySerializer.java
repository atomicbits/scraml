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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.atomicbits.scraml.dsl.androidjavajackson.DateTimeOnly;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by peter on 8/10/17.
 */
public class DateTimeOnlySerializer extends JsonSerializer<DateTimeOnly> {

    @Override
    public void serialize(DateTimeOnly value, JsonGenerator jgen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss[.SSS]", Locale.getDefault());

        if (value == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(format.format(value.getDateTime()));
        }
    }
}
