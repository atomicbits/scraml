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

package io.atomicbits.scraml.ramlparser.model

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

/**
  * Created by peter on 14/06/17.
  */
class FetchReplaceStringsTest extends AnyFeatureSpec with GivenWhenThen {

  Feature("Fetch the trait and resource type replace strings from their definition") {

    Scenario("fetch the replace strings form a trait or resource type definition") {

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
