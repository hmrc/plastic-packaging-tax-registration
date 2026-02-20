/*
 * Copyright 2026 HM Revenue & Customs
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

import config.AppConfig
import connectors.EnrolmentStoreProxyConnector.GroupsWithEnrolmentsTimerTag
import models.enrolment.EnrolmentKey
import models.enrolmentstoreproxy.GroupsWithEnrolmentsResponse
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK}
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpReadsInstances,
  HttpResponse,
  UpstreamErrorResponse
}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URI

@Singleton
class EnrolmentStoreProxyConnector @Inject() (
  httpClient: HttpClientV2,
  val config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances {

  /** ES1 **/
  def queryGroupsWithEnrolment(
    pptReference: String
  )(implicit hc: HeaderCarrier): Future[Option[GroupsWithEnrolmentsResponse]] = {
    val timer = metrics.defaultRegistry.timer(GroupsWithEnrolmentsTimerTag).time()

    httpClient.get(new URI(config.enrolmentStoreProxyES1QueryGroupsWithEnrolmentUrl(EnrolmentKey.create(pptReference))).toURL()).execute[HttpResponse]
    .map { response =>
      response.status match {
        case OK                     => Some(response.json.as[GroupsWithEnrolmentsResponse])
        case NO_CONTENT | NOT_FOUND => None
        case _                      => throw UpstreamErrorResponse(response.body, response.status)
      }
    }.andThen { case _ => timer.stop() }
  }

}

object EnrolmentStoreProxyConnector {
  val GroupsWithEnrolmentsTimerTag = "ppt.enrolment-store-proxy.groups-with-enrolments.timer"
}
