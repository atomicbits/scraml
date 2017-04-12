/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.codegen.DslSourceExtractor
import io.atomicbits.scraml.generator.platform.javajackson.JavaJackson
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.concurrent.ScalaFutures

/**
  * Created by peter on 12/04/17.
  */
class DslSourceExtractorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  feature("The DSL source extractor should be able to extract the DSL source code files") {

    scenario("test reading the DSL code") {

      Given("a DSL source file location")
      val sourceFile = "io.atomicbits.scraml.jdsl.BinaryData"

      DslSourceExtractor.extract(List(""))(JavaJackson)

      When("we read the file locatino")
      Then("we get the file contents")

    }
  }

}
