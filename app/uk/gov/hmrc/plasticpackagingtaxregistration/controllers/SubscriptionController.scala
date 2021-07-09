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

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegistrationRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepository
import uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs.NonRepudiationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  repository: RegistrationRepository,
  nonRepudiationService: NonRepudiationService,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def get(safeNumber: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      subscriptionsConnector.getSubscriptionStatus(safeNumber).map {
        response: SubscriptionStatusResponse =>
          Ok(response)
      }
    }

  def submit(safeNumber: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        val pptSubscription = request.body.toRegistration(request.pptId)
        val eisSubscription = Subscription(pptSubscription)
        logPayload("PPT Subscription: ", eisSubscription)
        subscriptionsConnector.submitSubscription(safeNumber, eisSubscription).map {
          case response @ SubscriptionCreateSuccessfulResponse(pptReference, processingDate, _) =>
            nonRepudiationService.submitNonRepudiation(toJson(pptSubscription).toString,
                                                       processingDate,
                                                       pptReference,
                                                       request.body.userHeaders.getOrElse(Map.empty)
            )
            repository.delete(request.pptId)
            Ok(response)
        }
    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
