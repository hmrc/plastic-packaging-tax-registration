/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.plasticpackagingtaxregistration.validators
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.SubscriptionTestData
import play.api.libs.json.Json
import play.api.libs.json.{JsResult, JsValue}

class PptSchemaValidatorSpec extends AnyWordSpec with Matchers with SubscriptionTestData {

    "validate" when {

        "given a valid create" should {

            "return success" in {

                val jsonRequest = Json.toJson(ukLimitedCompanySubscription)

                val sv = new PptSchemaValidator()

                val response = sv.validate("/api-docs/api-1711-ppt-subscription-create-1.6.0.json", jsonRequest)

                response.isSuccess mustBe true

            }

        }


        "given an invalid create" should {

            "return failure" in {

                val jsonRequest = Json.toJson(ukLimitedCompanySubscriptionInvalid)

                val sv = new PptSchemaValidator()

                val response: JsResult[JsValue] = sv.validate("/api-docs/api-1711-ppt-subscription-create-1.6.0.json", jsonRequest)

                response.isSuccess mustBe false
            }

        }

    }

}
