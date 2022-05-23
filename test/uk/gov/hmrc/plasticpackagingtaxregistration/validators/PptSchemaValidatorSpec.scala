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

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.SubscriptionTestData
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.libs.json.{JsResult, JsValue}
import play.api.libs.json.JsString
import play.api.libs.json.{JsPath, JsonValidationError}
import play.api.libs.json.JsArray
import scala.collection.mutable.WrappedArray
import uk.gov.hmrc.plasticpackagingtaxregistration.models.validation.JsonSchemaError

class PptSchemaValidatorSpec extends ScalaFutures with SubscriptionTestData with AnyWordSpecLike {

    "validate" when {

        "given a valid create" should {

            "return success" in {

                val jsonRequest = Json.toJson(ukLimitedCompanySubscription)

                val sv = new PptSchemaValidator()

                val response = sv.validate("/api-docs/api-1711-ppt-subscription-create-1.6.0.json", jsonRequest)

                println(response)

            }

        }


        "given an ivalid create" should {

            "return failure" in {

                val jsonRequest = Json.toJson(ukLimitedCompanySubscriptionInvalid)

                val sv = new PptSchemaValidator()

                val response: JsResult[JsValue] = sv.validate("/api-docs/api-1711-ppt-subscription-create-1.6.0.json", jsonRequest)

               response.fold(
                 errors => {
                   val asJson: JsValue = errors.flatMap(x => x._2).head.args.head.asInstanceOf[JsValue]

                   val foo = (asJson \ "errors").get

                   //val bar =  Json.fromJson[JsonSchemaError](foo)

                   println(foo)

                 },
                 clean => println("clean")
               )

            }

        }

    }
  
}
