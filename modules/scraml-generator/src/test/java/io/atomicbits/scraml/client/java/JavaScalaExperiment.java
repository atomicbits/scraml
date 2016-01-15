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

import io.atomicbits.scraml.client.manual.*;
import io.atomicbits.scraml.client.manual.PathparamResource;
import io.atomicbits.scraml.dsl.RequestBuilder;
import io.atomicbits.scraml.dsl.Response;
import scala.concurrent.Future;

/**
 * Created by peter on 28/08/15.
 */
public class JavaScalaExperiment {

    public void test() {
        PathparamResource resource = new
                PathparamResource("foo", new RequestBuilder(null, null, null, null, null, null, null, null));
        Future<Response<User>> user = resource.get(3, 2, null).call(null, null);

    }

}
