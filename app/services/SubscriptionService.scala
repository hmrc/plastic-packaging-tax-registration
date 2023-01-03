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

package services

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.HeaderCarrier
import models.eis.subscription.Subscription
import models.eis.subscription.create.{
  SubscriptionCreateWithEnrolmentAndNrsStatusesResponse,
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionSuccessfulResponse
}
import connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import connectors.{
  SubscriptionsConnector,
  TaxEnrolmentsConnector
}
import controllers.response.JSONResponses
import models.Registration
import models.nrs.NonRepudiationSubmissionAccepted
import repositories.RegistrationRepository
import services.nrs.NonRepudiationService
import util.Obfuscation.StringObfuscationOps
import validators.PptSchemaValidator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionService @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  enrolmentConnector: TaxEnrolmentsConnector,
  repository: RegistrationRepository,
  nonRepudiationService: NonRepudiationService
)(implicit ec: ExecutionContext)
    extends JSONResponses {
  private val logger = LoggerFactory.getLogger(getClass.getCanonicalName)

  def submit(pptRegistration: Registration, safeId: String, userHeaders: Map[String, String])(
    implicit hc: HeaderCarrier
  ): Future[Either[
    SubscriptionFailureResponseWithStatusCode,
    SubscriptionCreateWithEnrolmentAndNrsStatusesResponse
  ]] = {
    val pptSubscription = Subscription(pptRegistration, isSubscriptionUpdate = false)
    PptSchemaValidator.subscriptionValidator.validate(pptSubscription)
    subscriptionsConnector.submitSubscription(safeId, pptSubscription).flatMap {
      case subscriptionResponse @ SubscriptionSuccessfulResponse(pptReferenceNumber,
                                                                 _,
                                                                 formBundleNumber
          ) =>
        logger.info(
          s"Successful PPT subscription for ${pptSubscription.legalEntityDetails.name.obfuscated} with safeId ${safeId.obfuscated}, " +
            s"PPT Reference [${pptReferenceNumber.obfuscated}] FormBundleId [${formBundleNumber.obfuscated}]"
        )
        handleSuccessfulSubscription(subscriptionResponse,
                                     safeId,
                                     pptRegistration,
                                     userHeaders
        ).map(Right.apply _)

      case subscriptionResponse @ SubscriptionFailureResponseWithStatusCode(
            failedSubscriptionResponse,
            _
          ) =>
        val reasons = failedSubscriptionResponse.failures.map(_.reason)
        logger.error(
          s"Failed PPT subscription for ${pptSubscription.legalEntityDetails.name.obfuscated} with safeId ${safeId.obfuscated} - ${reasons.mkString("; ")}"
        )
        Future.successful(Left(subscriptionResponse))
    }
  }

  private def handleSuccessfulSubscription(
    subscriptionResponse: SubscriptionSuccessfulResponse,
    safeId: String,
    pptRegistration: Registration,
    userHeaders: Map[String, String]
  )(implicit hc: HeaderCarrier): Future[SubscriptionCreateWithEnrolmentAndNrsStatusesResponse] =
    for {
      enrolmentResponse <- enrolUser(subscriptionResponse.pptReferenceNumber,
                                     safeId,
                                     subscriptionResponse.formBundleNumber
      )
      nrsResponse <- notifyNRS(pptRegistration, subscriptionResponse, userHeaders)
      _           <- deleteRegistration(pptRegistration.id)
    } yield SubscriptionCreateWithEnrolmentAndNrsStatusesResponse(
      pptReference = subscriptionResponse.pptReferenceNumber,
      processingDate = subscriptionResponse.processingDate,
      formBundleNumber = subscriptionResponse.formBundleNumber,
      nrsNotifiedSuccessfully = nrsResponse.isSuccess,
      nrsSubmissionId = nrsResponse.fold(_ => None, nrsResponse => Some(nrsResponse.submissionId)),
      nrsFailureReason = nrsResponse.fold(e => Some(e.getMessage), _ => None),
      enrolmentInitiatedSuccessfully = enrolmentResponse.isSuccess
    )

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

  def notifyNRS(
    registration: Registration,
    subscriptionResponse: SubscriptionSuccessfulResponse,
    userHeaders: Map[String, String]
  )(implicit hc: HeaderCarrier): Future[Try[NonRepudiationSubmissionAccepted]] =
    nonRepudiationService.submitNonRepudiation(payloadString = toJson(registration).toString,
                                               submissionTimestamp =
                                                 subscriptionResponse.processingDate,
                                               pptReference =
                                                 subscriptionResponse.pptReferenceNumber,
                                               userHeaders = userHeaders
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

  private def deleteRegistration(registrationId: String) = repository.delete(registrationId)
}
