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
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReadsHttpResponse}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentStoreProxyConnector.KnownFactsTimerTag
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.UserEnrolmentRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy.{
  QueryKnownFactsRequest,
  QueryKnownFactsResponse
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject() (
  httpClient: HttpClient,
  val config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpReadsHttpResponse {

  /** ES20 **/
  def queryKnownFacts(
    userEnrolment: UserEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Option[QueryKnownFactsResponse]] = {
    val timer = metrics.defaultRegistry.timer(KnownFactsTimerTag).time()

    httpClient.POST[QueryKnownFactsRequest, Option[QueryKnownFactsResponse]](
      url = config.enrolmentStoreProxyE20Url,
      body = QueryKnownFactsRequest(userEnrolment)
    ).andThen { case _ => timer.stop() }
  }

}

object EnrolmentStoreProxyConnector {
  val KnownFactsTimerTag = "ppt.enrolment-store-proxy.known-facts.timer"
}
