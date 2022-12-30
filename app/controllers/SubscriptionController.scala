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

package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import connectors.SubscriptionsConnector
import models.eis.EISError
import models.eis.subscription.Subscription
import models.eis.subscription.create.{
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionSuccessfulResponse
}
import models.eis.subscription.update.SubscriptionUpdateWithNrsStatusResponse
import controllers.actions.Authenticator
import controllers.response.JSONResponses
import models.{Registration, RegistrationRequest}
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  subscriptionService: SubscriptionService,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def getStatus(safeNumber: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      subscriptionsConnector.getSubscriptionStatus(safeNumber).map {
        case Right(response) =>
          logPayload(s"PPT Subscription status response for safeId $safeNumber ", response)
          Ok(response)
        case Left(errorStatusCode) => new Status(errorStatusCode)
      }
    }

  def get(pptReference: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default, Some(pptReference)) { implicit request =>
      subscriptionsConnector.getSubscription(pptReference).map {
        case Right(response)       => Ok(Registration(response))
        case Left(errorStatusCode) => new Status(errorStatusCode)
      }
    }

  def update(pptReference: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest],
                                   Some(pptReference)
    ) {
      implicit request =>
        val updatedRegistration: Registration = request.body.toRegistration(request.registrationId)
        val updatedSubscription: Subscription =
          Subscription(updatedRegistration, isSubscriptionUpdate = true)
        subscriptionsConnector.updateSubscription(pptReference, updatedSubscription).flatMap {
          case response @ SubscriptionSuccessfulResponse(pptReferenceNumber, _, formBundleNumber) =>
            for {
              nrsResponse <- subscriptionService.notifyNRS(updatedRegistration,
                                                           response,
                                                           request.body.userHeaders
              )
            } yield Ok(
              SubscriptionUpdateWithNrsStatusResponse(
                pptReference =
                  response.pptReferenceNumber,
                processingDate = response.processingDate,
                formBundleNumber =
                  response.formBundleNumber,
                nrsNotifiedSuccessfully =
                  nrsResponse.isSuccess,
                nrsSubmissionId =
                  nrsResponse.fold(_ => None, nrsResponse => Some(nrsResponse.submissionId)),
                nrsFailureReason = nrsResponse.fold(e => Some(e.getMessage), _ => None)
              )
            )
          case SubscriptionFailureResponseWithStatusCode(failedSubscriptionResponse, statusCode) =>
            val firstError: EISError = failedSubscriptionResponse.failures.head
            logPayload(s"PPT Subscription update failed for pptReference $pptReference ",
                       failedSubscriptionResponse
            )
            logger.warn(
              s"Failed PPT update subscription for pptReference $pptReference - ${firstError.reason}"
            )
            Future.successful(Status(statusCode)(failedSubscriptionResponse))
        }
    }

  def submit(safeId: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        val pptRegistration = request.body.toRegistration(request.registrationId)
        subscriptionService.submit(pptRegistration, safeId, request.body.userHeaders) map {
          case Right(value) => Ok(value)
          case Left(value)  => Status(value.statusCode)(value.failureResponse)
        }
    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
