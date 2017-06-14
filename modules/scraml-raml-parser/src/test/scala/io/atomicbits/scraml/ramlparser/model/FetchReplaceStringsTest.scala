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

package io.atomicbits.scraml.ramlparser.model

import org.scalatest.{ FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 14/06/17.
  */
class FetchReplaceStringsTest extends FeatureSpec with GivenWhenThen {

  feature("Fetch the trait and resource type replace strings from their definition") {

    scenario("fetch the replace strings form a trait or resource type definition") {

      Given("a trait or resource type definition")
      val definition = "Return <<resourcePathName>> that have their <<queryParamName | !singularize>> matching the given value"

      When("we fetch the replace strings from the definition")
      val replaceStrings: Seq[ReplaceString] = ModelMergeTestImpl.fetchReplaceStrings(definition)

      Then("we get the expected replace string")
      replaceStrings shouldBe Seq(
        ReplaceString(toReplace   = "<<resourcePathName>>", matchString = "resourcePathName", operations = Seq(), partial = true),
        ReplaceString(toReplace   = "<<queryParamName | !singularize>>",
                      matchString = "queryParamName",
                      operations  = Seq(Singularize),
                      partial     = true)
      )

    }

  }

}

object ModelMergeTestImpl extends ModelMerge {}
