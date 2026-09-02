/*
 * Copyright 2026 HM Revenue & Customs
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

package models.hip.subscription.create

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, Json}

class HipSubscriptionFailureResponseSpec extends AnyWordSpec with Matchers {

  private val processingDate = "2026-07-09T09:26:17Z"
  private val logId          = "0123456789ABCDEF0123456789ABCDEF"

  private val businessValidationJson =
    Json.obj("error" -> Json.obj("processingDate" -> processingDate,
                                 "errorId" -> "007",
                                 "text"    -> "Business Partner already has active subscription for this regime"
    ))

  private val systemErrorJson = Json.obj("error" -> Json.obj("code" -> "500",
                                                             "message" -> "Internal Server Error",
                                                             "logID"   -> logId
  ))

  private val businessValidationFailure =
    HipBusinessValidationFailure(HipBusinessValidationError(processingDate, "007", "Business Partner already has active subscription for this regime"))

  private val systemFailure =
    HipSystemFailure(HipSystemError("500", "Internal Server Error", logId))

  "HipSubscriptionFailureResponse reads" should {
    "parse the business validation shape" in {
      businessValidationJson.validate[HipSubscriptionFailureResponse] mustBe JsSuccess(
        businessValidationFailure
      )
    }

    "parse the system error shape" in {
      systemErrorJson.validate[HipSubscriptionFailureResponse] mustBe JsSuccess(systemFailure)
    }

    "fail for a body matching neither shape" in {
      Json.obj("error" -> Json.obj("somethingElse" -> "xxx"))
        .validate[HipSubscriptionFailureResponse] mustBe a[JsError]
    }

    "fail for a body with no error object" in {
      Json.obj("xxx" -> "xxx").validate[HipSubscriptionFailureResponse] mustBe a[JsError]
    }
  }

  "HipSubscriptionFailureResponse writes" should {
    "round trip the business validation shape" in {
      val json = Json.toJson[HipSubscriptionFailureResponse](businessValidationFailure)

      json mustBe businessValidationJson
      json.as[HipSubscriptionFailureResponse] mustBe businessValidationFailure
    }

    "round trip the system error shape" in {
      val json = Json.toJson[HipSubscriptionFailureResponse](systemFailure)

      json mustBe systemErrorJson
      json.as[HipSubscriptionFailureResponse] mustBe systemFailure
    }
  }

  "HipSubscriptionFailureResponseWithStatusCode" should {
    "reproduce the business validation body and render its reason" in {
      val failure = HipSubscriptionFailureResponseWithStatusCode(businessValidationFailure,
                                                                 Status.UNPROCESSABLE_ENTITY
      )

      failure.statusCode mustBe Status.UNPROCESSABLE_ENTITY
      failure.failureJson mustBe businessValidationJson
      failure.failureReasons mustBe Seq("[007] Business Partner already has active subscription for this regime")
    }

    "reproduce the system error body and render its reason" in {
      val failure =
        HipSubscriptionFailureResponseWithStatusCode(systemFailure, Status.INTERNAL_SERVER_ERROR)

      failure.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      failure.failureJson mustBe systemErrorJson
      failure.failureReasons mustBe Seq("[500] Internal Server Error")
    }
  }

}
