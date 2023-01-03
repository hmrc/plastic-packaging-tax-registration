/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import play.api.http.Status
import play.api.libs.json.{JsResultException, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, route, writeableOf_AnyContentAsJson}
import base.unit.ControllerSpec
import builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}
import models.EnrolmentStatus

class TaxEnrolmentsControllerSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  "Tax Enrolments Controller" should {
    val pptReference = "PPTRef"
    val post         = FakeRequest("POST", s"/tax-enrolments-callback/$pptReference")

    "return a 204 status when notified of successful enrolment" in {
      val enrolmentStatus = Json.obj("state" -> EnrolmentStatus.Success.jsonName)

      val result = await(route(app, post.withJsonBody(enrolmentStatus)).get)

      result.header.status mustBe Status.NO_CONTENT
    }

    "return a 204 status when notified of failed enrolments" in {
      val failedEnrolmentStatuses = List(EnrolmentStatus.Failure.jsonName,
                                         EnrolmentStatus.Enrolled.jsonName,
                                         EnrolmentStatus.EnrolmentError.jsonName,
                                         EnrolmentStatus.AuthRefreshed.jsonName
      )

      failedEnrolmentStatuses.foreach { enrolmentStatus =>
        val enrolmentStatusNotification = Json.obj("state" -> enrolmentStatus)

        val result = await(route(app, post.withJsonBody(enrolmentStatusNotification)).get)

        result.header.status mustBe Status.NO_CONTENT
      }
    }

    "throw JsResultException when malformed request received" in {
      val enrolmentStatusNotification = Json.obj("xxx" -> "XXX")

      intercept[JsResultException] {
        await(route(app, post.withJsonBody(enrolmentStatusNotification)).get)
      }
    }
  }
}
