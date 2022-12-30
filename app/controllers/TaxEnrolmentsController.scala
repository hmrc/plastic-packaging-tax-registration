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

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import controllers.response.JSONResponses
import models.EnrolmentStatus
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

case class SubscriptionState(state: String, error: String)

@Singleton
class TaxEnrolmentsController @Inject() (override val controllerComponents: ControllerComponents)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def callback(pptReference: String): Action[JsValue] =
    Action.async(parse.json) {
      implicit req =>
        val enrolmentStatus = (req.body \ "state").as[EnrolmentStatus]
        logger.info(s"Enrolment complete for $pptReference, enrolment state = $enrolmentStatus")
        Future.successful(NoContent)
    }

}
