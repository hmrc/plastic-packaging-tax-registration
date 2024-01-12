/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import models.KeyValue
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Authenticator @Inject() (override val authConnector: AuthConnector, cc: ControllerComponents)(
  implicit ec: ExecutionContext
) extends BackendController(cc) with AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  def parsingJson[T](implicit rds: Reads[T]): BodyParser[T] =
    parse.json.validate { json =>
      json.validate[T] match {
        case JsSuccess(value, _) => Right(value)
        case JsError(error) =>
          val errorResponse = Json.toJson(ErrorResponse(BAD_REQUEST, "Bad Request"))
          logger.warn(s"Bad Request [$errorResponse]")
          logger.warn(s"Errors: [$error]")
          Left(BadRequest(errorResponse))
      }
    }

  def authorisedAction[A](bodyParser: BodyParser[A], pptReference: Option[String] = None)(
    body: AuthorizedRequest[A] => Future[Result]
  ): Action[A] =
    Action.async(bodyParser) { implicit request =>
      authorisedWithInternalIdAndGroupIdentifier(pptReference).flatMap {
        case Right(authorisedRequest) =>
          logger.info(s"Authorised request for ${authorisedRequest.registrationId}")
          body(authorisedRequest)
        case Left(error) =>
          logger.warn(s"Problems with Authorisation: ${error.message}")
          Future.successful(Unauthorized(error.message))
      }
    }

  def authorisedWithInternalIdAndGroupIdentifier[A](pptReference: Option[String] = None)(implicit
    hc: HeaderCarrier,
    request: Request[A]
  ): Future[Either[ErrorResponse, AuthorizedRequest[A]]] = {

    val enrolmentPredicate = pptReference.map { pptReference =>
      Enrolment(KeyValue.pptServiceName).withDelegatedAuthRule("ppt-auth").withIdentifier(
        KeyValue.etmpPptReferenceKey,
        pptReference
      )
    }.getOrElse(EmptyPredicate)

    authorised(enrolmentPredicate).retrieve(internalId and credentials and groupIdentifier) {
      case Some(internalId) ~ Some(credentials) ~ Some(groupIdentifier) =>
        Future.successful(
          Right(AuthorizedRequest(internalId, credentials.providerId, groupIdentifier, request))
        )
      case _ =>
        val msg = "Unauthorised access. User without an HMRC Internal Id and/or Group Identifier"
        logger.warn(msg)
        Future.successful(Left(ErrorResponse(UNAUTHORIZED, msg)))
    } recover {
      case error: AuthorisationException =>
        logger.warn(s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse(UNAUTHORIZED, "Unauthorized for plastic packaging tax"))
      case ex: Throwable =>
        val msg = "Internal server error is " + ex.getMessage
        logger.warn(msg)
        Left(ErrorResponse(INTERNAL_SERVER_ERROR, msg))
    }
  }

}

case class AuthorizedRequest[A](
  registrationId: String,
  userId: String,
  groupId: String,
  request: Request[A]
) extends WrappedRequest[A](request)
