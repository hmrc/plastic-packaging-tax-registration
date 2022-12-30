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
import play.api.mvc._
import controllers.actions.Authenticator
import controllers.response.JSONResponses
import models.RegistrationRequest
import repositories.RegistrationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject() (
  registrationRepository: RegistrationRepository,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {
  private val logger = Logger(this.getClass)

  def get(id: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { _ =>
      registrationRepository.findByRegistrationId(id).map {
        case Some(registration) => Ok(registration)
        case None               => NotFound
      }
    }

  def create(): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        logPayload("Create Registration Request Received", request.body)
        registrationRepository
          .create(request.body.toRegistration(request.registrationId))
          .map(logPayload("Create Registration Response", _))
          .map(registration => Created(registration))
    }

  def update(id: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) {
      implicit request =>
        logPayload("Update Registration Request Received", request.body)
        registrationRepository.findByRegistrationId(id).flatMap {
          case Some(_) =>
            registrationRepository
              .update(request.body.toRegistration(request.registrationId))
              .map(logPayload("Update Registration Response", _))
              .map {
                case Some(registration) => Ok(registration)
                case None               => NotFound
              }
          case None =>
            logPayload("Update Registration Response", "Not Found")
            Future.successful(NotFound)
        }
    }

  private def logPayload[T](prefix: String, payload: T): T = {
    logger.debug(s"$prefix payload: $payload")
    payload
  }

}
