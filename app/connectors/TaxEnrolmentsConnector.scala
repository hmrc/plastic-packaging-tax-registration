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

package connectors

import com.kenshoo.play.metrics.Metrics
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpReadsHttpResponse,
  HttpResponse,
  UpstreamErrorResponse
}
import config.AppConfig
import connectors.TaxEnrolmentsConnector._
import models.KeyValue.pptServiceName
import models.enrolment.{EnrolmentKey, KnownFacts, UserEnrolmentRequest}
import models.taxenrolments.GroupEnrolment
import connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import controllers.routes

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsConnector @Inject() (
  httpClient: HttpClient,
  val config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpReadsHttpResponse {

  private val logger = Logger(this.getClass)

  /** Async ROSM "Subscriber" call **/
  def submitEnrolment(pptReference: String, safeId: String, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[TaxEnrolmentsResponse] = {

    def taxEnrolmentsCallbackUrl(pptReference: String): String =
      s"${config.selfHost}${routes.TaxEnrolmentsController.callback(pptReference).url}"

    val timer = metrics.defaultRegistry.timer(SubscriberTimerTag).time()
    val enrolmentRequestBody =
      Json.obj("serviceName" -> pptServiceName,
               "callback"    -> taxEnrolmentsCallbackUrl(pptReference),
               "etmpId"      -> safeId
      )

    httpClient.PUT[JsObject, TaxEnrolmentsResponse](
      url = config.taxEnrolmentsSubscriptionsSubscriberUrl(formBundleId),
      body = enrolmentRequestBody
    ).andThen { case _ => timer.stop() }
  }

  /** ES11 **/
  def assignEnrolmentToUser(userId: String, pptReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val timer = metrics.defaultRegistry.timer(AssignEnrolmentToUserTimerTag).time()

    httpClient.POSTEmpty[HttpResponse](url =
      config.taxEnrolmentsES11AssignUserToEnrolmentUrl(userId, EnrolmentKey.create(pptReference))
    ).map { resp =>
      resp.status match {
        case status if Status.isSuccessful(status) =>
          logger.info(
            s"ES11 successful assign enrolment to user with userId [$userId] and pptReference [$pptReference]"
          )
          ()
        // Do nothing - return without exception
        case otherStatus =>
          logger.warn(
            s"ES11 failed assign enrolment to user with userId [$userId] and pptReference [$pptReference] with status $otherStatus"
          )
          throw UpstreamErrorResponse(AssignEnrolmentToUserError, otherStatus)
      }
    }.andThen { case _ => timer.stop() }
  }

  /** ES8 **/
  def assignEnrolmentToGroup(
    userId: String,
    groupId: String,
    userEnrolmentRequest: UserEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val timer = metrics.defaultRegistry.timer(AssignEnrolmentToGroupTimerTag).time()

    val body =
      GroupEnrolment(userId = userId, verifiers = KnownFacts.from(userEnrolmentRequest))

    httpClient.POST[GroupEnrolment, HttpResponse](
      url = config.taxEnrolmentsES8AssignUserToGroupUrl(
        groupId,
        EnrolmentKey.create(userEnrolmentRequest.pptReference)
      ),
      body = body
    ).map { resp =>
      resp.status match {
        case status if Status.isSuccessful(status) =>
          logger.info(
            s"ES8 successful assign enrolment to group with userId [$userId], groupId [$groupId] and pptReference [${userEnrolmentRequest.pptReference}]"
          )
          ()
        // Do nothing - return without exception
        case otherStatus =>
          logger.warn(
            s"ES8 failed assign enrolment to user with userId [$userId] and pptReference [${userEnrolmentRequest.pptReference}] with status [$otherStatus]"
          )
          throw UpstreamErrorResponse(AssignEnrolmentToGroupError, otherStatus)
      }
    }.andThen { case _ => timer.stop() }

  }

}

object TaxEnrolmentsConnector {
  val SubscriberTimerTag             = "ppt.tax-enrolments.subscriber.timer"
  val AssignEnrolmentToUserTimerTag  = "ppt.tax-enrolments.assign-enrolment-user.timer"
  val AssignEnrolmentToGroupTimerTag = "ppt.tax-enrolments.assign-enrolment-group.timer"

  val AssignEnrolmentToUserError  = "User enrolment assignment failed"
  val AssignEnrolmentToGroupError = "Enrolment to group failed"
}
