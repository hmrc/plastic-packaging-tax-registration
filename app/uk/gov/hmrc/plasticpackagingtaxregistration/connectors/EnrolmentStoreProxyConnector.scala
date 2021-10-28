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
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentStoreProxyConnector.{
  GroupsWithEnrolmentsTimerTag,
  KnownFactsTimerTag
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.UserEnrolmentRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy.{
  GroupsWithEnrolmentsResponse,
  KeyValue,
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

  /** ES1 **/
  def queryGroupsWithEnrolment(
    pptReference: String
  )(implicit hc: HeaderCarrier): Future[Option[GroupsWithEnrolmentsResponse]] = {
    val timer = metrics.defaultRegistry.timer(GroupsWithEnrolmentsTimerTag).time()

    val enrolmentKey = s"${KeyValue.pptServiceName}~${KeyValue.etmpPptReferenceKey}~$pptReference"

    httpClient.GET[Option[GroupsWithEnrolmentsResponse]](url =
      config.enrolmentStoreProxyES1Url(enrolmentKey)
    ).andThen { case _ => timer.stop() }
  }

  /** ES20 **/
  def queryKnownFacts(
    userEnrolment: UserEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Option[QueryKnownFactsResponse]] = {
    val timer = metrics.defaultRegistry.timer(KnownFactsTimerTag).time()

    httpClient.POST[QueryKnownFactsRequest, Option[QueryKnownFactsResponse]](
      url = config.enrolmentStoreProxyES20Url,
      body = QueryKnownFactsRequest(userEnrolment)
    ).andThen { case _ => timer.stop() }
  }

}

object EnrolmentStoreProxyConnector {
  val KnownFactsTimerTag           = "ppt.enrolment-store-proxy.known-facts.timer"
  val GroupsWithEnrolmentsTimerTag = "ppt.enrolment-store-proxy.groups-with-enrolments.timer"
}
