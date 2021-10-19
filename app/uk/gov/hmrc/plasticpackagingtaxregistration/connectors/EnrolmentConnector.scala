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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReadsHttpResponse}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentConnector.{
  EnrolmentConnectorTimerTag,
  PPTServiceName
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.routes

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentConnector @Inject() (
  httpClient: HttpClient,
  val config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpReadsHttpResponse {

  def submitEnrolment(pptReference: String, safeId: String, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[TaxEnrolmentsResponse] = {
    val timer = metrics.defaultRegistry.timer(EnrolmentConnectorTimerTag).time()
    val enrolmentRequestBody =
      Json.obj("serviceName" -> PPTServiceName,
               "callback"    -> taxEnrolmentsCallbackUrl(pptReference),
               "etmpId"      -> safeId
      )

    httpClient.PUT[JsObject, TaxEnrolmentsResponse](
      url = config.getTaxEnrolmentsSubscriberUrl(formBundleId),
      body = enrolmentRequestBody
    ).andThen { case _ => timer.stop() }
  }

  private def taxEnrolmentsCallbackUrl(pptReference: String): String =
    s"${config.selfHost}${routes.EnrolmentController.enrolled(pptReference).url}"

}

object EnrolmentConnector {
  val PPTServiceName             = "HMRC-PPT-ORG"
  val EnrolmentConnectorTimerTag = "ppt.enrolment.timer"
}
