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

package io.atomicbits.scraml.client.generated

import io.atomicbits.scraml.generator.ScramlGenerator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, FeatureSpec}

/**
 * Created by peter on 29/08/15. 
 */
class TestApiTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  feature("The scraml generator generates DSL classes") {

    scenario("test generated Scala DSL") {

      Given("")


      When("")
      val apiResourceUrl = this.getClass.getClassLoader.getResource("io/atomicbits/scraml/TestApi.raml")

      val generatedFiles =
        ScramlGenerator.generate(
          ramlApiPath = apiResourceUrl.toString,
          apiPackageName = "io.atomicbits.scraml",
          apiClassName = "TestApi"
        )

      Then("")
       println(generatedFiles)

    }

  }

}
