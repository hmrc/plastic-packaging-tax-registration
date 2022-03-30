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

package uk.gov.hmrc.plasticpackagingtaxregistration.controllers

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json.Writes
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.create.{
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.update.SubscriptionUpdateWithNrsStatusResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  ChangeOfCircumstanceDetails,
  DeregistrationDetails,
  Subscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.plasticpackagingtaxregistration.models.DeregistrationReason.DeregistrationReason
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs.NonRepudiationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class DeregistrationController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  nonRepudiationService: NonRepudiationService,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def deregister(pptReference: String): Action[DeregistrationReason] =
    authenticator.authorisedAction(authenticator.parsingJson[DeregistrationReason]) {
      implicit request =>
        val deregistrationReason: DeregistrationReason = request.body
        logger.info(s"Request to deregister $pptReference, reason = $deregistrationReason")

        subscriptionsConnector.getSubscription(pptReference).flatMap {
          case Right(subscription) =>
            val updatedSubscription = deregisterSubscription(subscription, deregistrationReason)
            subscriptionsConnector.updateSubscription(pptReference, updatedSubscription).flatMap {
              case response @ SubscriptionSuccessfulResponse(pptReferenceNumber,
                                                             _,
                                                             formBundleNumber
                  ) =>
                for {
                  nrsResponse <- notifyNRS(updatedSubscription, response)
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
              case SubscriptionFailureResponseWithStatusCode(failedSubscriptionResponse,
                                                             statusCode
                  ) =>
                val firstError: EISError = failedSubscriptionResponse.failures.head
                logPayload(
                  s"PPT Subscription deregistration failed for pptReference $pptReference ",
                  failedSubscriptionResponse
                )
                logger.warn(
                  s"Failed PPT update deregistration for pptReference $pptReference - ${firstError.reason}"
                )
                Future.successful(Status(statusCode)(failedSubscriptionResponse))
            }
          case Left(errorCode) => Future.successful(new Status(errorCode))
        }
    }

  private def deregisterSubscription(
    subscription: Subscription,
    deregistrationReason: DeregistrationReason
  ) =
    subscription.copy(changeOfCircumstanceDetails =
      Some(
        ChangeOfCircumstanceDetails(changeOfCircumstance = "Deregistration",
                                    deregistrationDetails = Some(
                                      DeregistrationDetails(
                                        deregistrationReason = deregistrationReason.toString,
                                        deregistrationDate = LocalDate.now(ZoneOffset.UTC).toString,
                                        deregistrationDeclarationBox1 = true
                                      )
                                    )
        )
      )
    )

  private def notifyNRS(
    subscription: Subscription,
    subscriptionResponse: SubscriptionSuccessfulResponse
  )(implicit hc: HeaderCarrier): Future[Try[NonRepudiationSubmissionAccepted]] =
    nonRepudiationService.submitNonRepudiation(payloadString = toJson(subscription).toString,
                                               submissionTimestamp =
                                                 subscriptionResponse.processingDate,
                                               pptReference =
                                                 subscriptionResponse.pptReferenceNumber,
                                               userHeaders =
                                                 Map.empty
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
