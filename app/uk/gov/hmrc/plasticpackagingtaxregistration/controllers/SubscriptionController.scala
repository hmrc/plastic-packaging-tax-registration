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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateFailureResponseWithStatusCode,
  SubscriptionCreateSuccessfulResponse,
  SubscriptionCreateWithEnrolmentAndNrsStatusesResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.{
  SubscriptionsConnector,
  TaxEnrolmentsConnector
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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SubscriptionController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  repository: RegistrationRepository,
  nonRepudiationService: NonRepudiationService,
  enrolmentConnector: TaxEnrolmentsConnector,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def get(safeNumber: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      subscriptionsConnector.getSubscriptionStatus(safeNumber).map { response =>
        logPayload(s"PPT Subscription status response for safeId $safeNumber ", response)
        Ok(response)
      }
    }

  def submit(safeId: Option[String]): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        val pptRegistration = request.body.toRegistration(request.registrationId)
        val pptSubscription = Subscription(pptRegistration)
        logPayload(s"PPT Subscription Create request for safeId $safeId ", pptSubscription)

        subscriptionsConnector.submitSubscription(safeId, pptSubscription)
          .flatMap {
            case subscriptionResponse @ SubscriptionCreateSuccessfulResponse(pptReferenceNumber,
                                                                             _,
                                                                             formBundleNumber
                ) =>
              logger.info(
                s"Successful PPT subscription for ${pptSubscription.legalEntityDetails.name} with safeId $safeId, " +
                  s"PPT Reference [$pptReferenceNumber] FormBundleId [$formBundleNumber]"
              )
              for {
                enrolmentResponse <- enrolUser(pptReferenceNumber, safeId, formBundleNumber)
                nrsResponse       <- notifyNRS(request, pptRegistration, subscriptionResponse)
                _                 <- deleteRegistration(request.registrationId)
              } yield Ok(
                SubscriptionCreateWithEnrolmentAndNrsStatusesResponse(
                  pptReference = pptReferenceNumber,
                  processingDate =
                    subscriptionResponse.processingDate,
                  formBundleNumber =
                    subscriptionResponse.formBundleNumber,
                  nrsNotifiedSuccessfully =
                    nrsResponse.isSuccess,
                  nrsSubmissionId =
                    nrsResponse.fold(_ => None, nrsResponse => Some(nrsResponse.submissionId)),
                  nrsFailureReason =
                    nrsResponse.fold(e => Some(e.getMessage), _ => None),
                  enrolmentInitiatedSuccessfully =
                    enrolmentResponse.isSuccess
                )
              )
            case SubscriptionCreateFailureResponseWithStatusCode(failedSubscriptionResponse,
                                                                 statusCode
                ) =>
              val firstError = failedSubscriptionResponse.failures.head
              logPayload(s"PPT Subscription Create failed response for safeId $safeId ",
                         failedSubscriptionResponse
              )
              logger.warn(
                s"Failed PPT subscription for ${pptSubscription.legalEntityDetails.name} with safeId $safeId - ${firstError.reason}"
              )
              Future.successful(Status(statusCode)(failedSubscriptionResponse))
          }
    }

  private def deleteRegistration(registrationId: String) =
    repository.delete(registrationId)

  private def enrolUser(pptReference: String, safeId: String, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[Try[TaxEnrolmentsResponse]] =
    enrolmentConnector.submitEnrolment(pptReference, safeId, formBundleId)
      .map {
        case Right(successfulTaxEnrolment) =>
          logger.info(
            s"Successful subscriber enrolment initiation for PPT Reference [$pptReference], Safe ID [$safeId], FormBundleId [$formBundleId]"
          )
          Success(Right(successfulTaxEnrolment))
        case Left(failedTaxEnrolment) =>
          logger.warn(
            s"Failed subscriber enrolment initiation for PPT Reference [$pptReference], Safe ID [$safeId], FormBundleId [$formBundleId] " +
              s"- error code [${failedTaxEnrolment.status}]"
          )
          Failure(new IllegalStateException("Enrolment failed"))
      }
      .recover {
        case e =>
          logger.warn(
            s"Failed subscriber enrolment initiation for PPT Reference [$pptReference], Safe ID [$safeId], FormBundleId [$formBundleId] " +
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
                                               pptReference =
                                                 subscriptionResponse.pptReferenceNumber,
                                               userHeaders =
                                                 request.body.userHeaders.getOrElse(Map.empty)
    )
      .map {
        resp =>
          logger.info(
            s"Successful NRS submission for PPT Reference [${subscriptionResponse.pptReferenceNumber}]"
          )
          Success(resp)
      }
      .recover {
        case e =>
          logger.warn(
            s"Failed NRS submission for PPT Reference [${subscriptionResponse.pptReferenceNumber}] - ${e.getMessage}"
          )
          Failure(e)
      }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
