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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors

import com.kenshoo.play.metrics.Metrics
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpReadsHttpResponse,
  HttpResponse,
  UpstreamErrorResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.TaxEnrolmentsConnector.{
  AssignEnrolmentTimerTag,
  SubscriberTimerTag
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy.KeyValue.{
  etmpPptReferenceKey,
  pptServiceName
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.routes

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsConnector @Inject() (
  httpClient: HttpClient,
  val config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpReadsHttpResponse {

  def submitEnrolment(pptReference: String, safeId: String, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[TaxEnrolmentsResponse] = {
    val timer = metrics.defaultRegistry.timer(SubscriberTimerTag).time()
    val enrolmentRequestBody =
      Json.obj("serviceName" -> pptServiceName,
               "callback"    -> taxEnrolmentsCallbackUrl(pptReference),
               "etmpId"      -> safeId
      )

    httpClient.PUT[JsObject, TaxEnrolmentsResponse](
      url = config.getTaxEnrolmentsSubscriberUrl(formBundleId),
      body = enrolmentRequestBody
    ).andThen { case _ => timer.stop() }
  }

  def assignEnrolmentToUser(userId: String, pptReference: String)(implicit hc: HeaderCarrier) = {
    val timer = metrics.defaultRegistry.timer(AssignEnrolmentTimerTag).time()
    httpClient.POSTEmpty[HttpResponse](url =
      config.getTaxEnrolmentsAssignUserToEnrolmentUrl(userId, enrolmentKey(pptReference))
    ).map { resp =>
      resp.status match {
        case Status.CREATED => // Do nothing - return without exception
        case otherStatus =>
          throw UpstreamErrorResponse("User enrolment assignment failed", otherStatus)
      }
    }.andThen { case _ => timer.stop() }
  }

  private def taxEnrolmentsCallbackUrl(pptReference: String): String =
    s"${config.selfHost}${routes.TaxEnrolmentsController.callback(pptReference).url}"

  private def enrolmentKey(pptReference: String) =
    s"$pptServiceName~$etmpPptReferenceKey~$pptReference"

}

object TaxEnrolmentsConnector {
  val SubscriberTimerTag      = "ppt.tax-enrolments.subscriber.timer"
  val AssignEnrolmentTimerTag = "ppt.tax-enrolments.assign-enrolment.timer"
}
