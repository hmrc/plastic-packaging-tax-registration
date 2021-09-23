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
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateFailureResponseWithStatusCode,
  SubscriptionCreateSuccessfulResponse,
  SubscriptionCreateWithEnrolmentAndNrsStatusesResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.{
  EnrolmentConnector,
  SubscriptionsConnector
}
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
import scala.util.{Failure, Success, Try}

@Singleton
class SubscriptionController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  repository: RegistrationRepository,
  nonRepudiationService: NonRepudiationService,
  enrolmentConnector: EnrolmentConnector,
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

  def submit(safeId: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        val pptRegistration = request.body.toRegistration(request.registrationId)
        val pptSubscription = Subscription(pptRegistration)
        logPayload("PPT Subscription: ", pptSubscription)

        subscriptionsConnector.submitSubscription(safeId, pptSubscription)
          .flatMap {
            case subscriptionResponse @ SubscriptionCreateSuccessfulResponse(pptReference, _, _) =>
              logger.info(
                s"Successful PPT subscription for ${pptSubscription.legalEntityDetails.name}, " +
                  s"PPT Reference [${subscriptionResponse.pptReference}]"
              )
              for {
                enrolmentResponse <- enrolUser(pptReference, safeId)
                nrsResponse       <- notifyNRS(request, pptRegistration, subscriptionResponse)
                _                 <- deleteRegistration(request.registrationId)
              } yield Ok(
                SubscriptionCreateWithEnrolmentAndNrsStatusesResponse(pptReference = pptReference,
                                                                      processingDate =
                                                                        subscriptionResponse.processingDate,
                                                                      formBundleNumber =
                                                                        subscriptionResponse.formBundleNumber,
                                                                      nrsNotifiedSuccessfully =
                                                                        nrsResponse.isSuccess,
                                                                      nrsSubmissionId =
                                                                        nrsResponse.fold(
                                                                          _ => None,
                                                                          nrsResponse =>
                                                                            Some(
                                                                              nrsResponse.submissionId
                                                                            )
                                                                        ),
                                                                      nrsFailureReason =
                                                                        nrsResponse.fold(
                                                                          e => Some(e.getMessage),
                                                                          _ => None
                                                                        ),
                                                                      enrolmentInitiatedSuccessfully =
                                                                        enrolmentResponse.isSuccess
                )
              )
            case SubscriptionCreateFailureResponseWithStatusCode(failedSubscriptionResponse,
                                                                 statusCode
                ) =>
              val firstError = failedSubscriptionResponse.failures.head
              logger.warn(
                s"Failed PPT subscription for ${pptSubscription.legalEntityDetails.name} - ${firstError.reason}"
              )
              Future.successful(Status(statusCode)(failedSubscriptionResponse))
          }
    }

  private def deleteRegistration(registrationId: String) =
    repository.delete(registrationId)

  private def enrolUser(pptReference: String, safeId: String)(implicit
    hc: HeaderCarrier
  ): Future[Try[TaxEnrolmentsResponse]] =
    enrolmentConnector.submitEnrolment(pptReference, safeId)
      .map {
        case Right(successfulTaxEnrolment) =>
          logger.info(
            s"Successful subscriber enrolment initiation for PPT Reference [$pptReference] and Safe ID [$safeId]"
          )
          Success(Right(successfulTaxEnrolment))
        case Left(failedTaxEnrolment) =>
          logger.warn(
            s"Failed subscriber enrolment initiation for PPT Reference [$pptReference] and Safe ID [$safeId] " +
              s"- error code [${failedTaxEnrolment.status}]"
          )
          Failure(new IllegalStateException("Enrolment failed"))
      }
      .recover {
        case e =>
          logger.warn(
            s"Failed subscriber enrolment initiation for PPT Reference [$pptReference] and Safe ID [$safeId] " +
              s"- ${e.getMessage}"
          )
          Failure(e)
      }

  private def notifyNRS(
    request: AuthorizedRequest[RegistrationRequest],
    registration: Registration,
    subscriptionResponse: SubscriptionCreateSuccessfulResponse
  )(implicit hc: HeaderCarrier): Future[Try[NonRepudiationSubmissionAccepted]] =
    nonRepudiationService.submitNonRepudiation(payloadString = toJson(registration).toString,
                                               submissionTimestamp =
                                                 subscriptionResponse.processingDate,
                                               pptReference = subscriptionResponse.pptReference,
                                               userHeaders =
                                                 request.body.userHeaders.getOrElse(Map.empty)
    )
      .map {
        resp =>
          logger.info(
            s"Successful NRS submission for PPT Reference [${subscriptionResponse.pptReference}]"
          )
          Success(resp)
      }
      .recover {
        case e =>
          logger.warn(
            s"Failed NRS submission for PPT Reference [${subscriptionResponse.pptReference}] - ${e.getMessage}"
          )
          Failure(e)
      }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
