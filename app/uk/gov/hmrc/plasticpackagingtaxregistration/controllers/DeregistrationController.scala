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
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  ChangeOfCircumstanceDetails,
  DeregistrationDetails,
  Subscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.plasticpackagingtaxregistration.models.DeregistrationReason.DeregistrationReason
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregistrationController @Inject() (
  subscriptionsConnector: SubscriptionsConnector,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def deregister(pptReference: String): Action[DeregistrationReason] =
    authenticator.authorisedAction(authenticator.parsingJson[DeregistrationReason]) {
      implicit request =>
        val deregistrationReason = request.body
        logger.info(s"Request to deregister $pptReference, reason = $deregistrationReason")

        subscriptionsConnector.getSubscription(pptReference).flatMap {
          case Right(subscription) =>
            subscriptionsConnector.updateSubscription(pptReference,
                                                      deregisterSubscription(subscription,
                                                                             deregistrationReason
                                                      )
            ).map { _ =>
              Ok
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

}
