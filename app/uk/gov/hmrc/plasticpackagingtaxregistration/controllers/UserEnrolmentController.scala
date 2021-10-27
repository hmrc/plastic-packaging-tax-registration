/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtaxregistration.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.{
  EnrolmentFailedCode,
  UserEnrolment,
  UserEnrolmentFailedResponse,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserEnrolmentController @Inject() (
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def enroll(pptReference: String): Action[UserEnrolment] =
    authenticator.authorisedAction(authenticator.parsingJson[UserEnrolment]) {
      implicit request =>
        val userEnrolment = request.body
        logPayload(s"PPT User Enroll request for pptReference $pptReference ", userEnrolment)

        // TODO temporary mocking of result
        if (pptReference.endsWith("000"))
          Future.successful(
            BadRequest(UserEnrolmentFailedResponse(pptReference, EnrolmentFailedCode.Failed))
          )
        else
          Future.successful(Ok(UserEnrolmentSuccessResponse(pptReference)))

    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
