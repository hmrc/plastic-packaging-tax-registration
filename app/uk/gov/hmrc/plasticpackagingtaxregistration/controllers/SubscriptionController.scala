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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateSuccessfulResponse,
  SubscriptionCreateWithNrsFailureResponse,
  SubscriptionCreateWithNrsSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.{
  Authenticator,
  AuthorizedRequest
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{Registration, RegistrationRequest}
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepository
import uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs.NonRepudiationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
        subscriptionsConnector.submitSubscription(safeNumber, eisSubscription).flatMap {
          case eisResponse @ SubscriptionCreateSuccessfulResponse(_, _, _) =>
            handleNrsRequest(request, pptSubscription, eisResponse)
              .andThen { case _ => repository.delete(request.pptId) }

        }
    }

  private def handleNrsRequest(
    request: AuthorizedRequest[RegistrationRequest],
    pptSubscription: Registration,
    eisResponse: SubscriptionCreateSuccessfulResponse
  )(implicit hc: HeaderCarrier): Future[Result] =
    submitToNrs(request, pptSubscription, eisResponse).map {
      case NonRepudiationSubmissionAccepted(nrSubmissionId) =>
        handleNrsSuccess(eisResponse, nrSubmissionId)
    }.recoverWith {
      case exception: Exception =>
        handleNrsFailure(eisResponse, exception)
    }

  private def submitToNrs(
    request: AuthorizedRequest[RegistrationRequest],
    pptSubscription: Registration,
    eisResponse: SubscriptionCreateSuccessfulResponse
  )(implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionAccepted] =
    nonRepudiationService.submitNonRepudiation(toJson(pptSubscription).toString,
                                               eisResponse.processingDate,
                                               eisResponse.pptReference,
                                               request.body.userHeaders.getOrElse(Map.empty)
    )

  private def handleNrsFailure(
    eisResponse: SubscriptionCreateSuccessfulResponse,
    exception: Exception
  ): Future[Result] =
    Future.successful(
      Ok(
        SubscriptionCreateWithNrsFailureResponse(eisResponse.pptReference,
                                                 eisResponse.processingDate,
                                                 eisResponse.formBundleNumber,
                                                 exception.getMessage
        )
      )
    )

  private def handleNrsSuccess(
    eisResponse: SubscriptionCreateSuccessfulResponse,
    nrSubmissionId: String
  ): Result =
    Ok(
      SubscriptionCreateWithNrsSuccessfulResponse(eisResponse.pptReference,
                                                  eisResponse.processingDate,
                                                  eisResponse.formBundleNumber,
                                                  nrSubmissionId
      )
    )

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
