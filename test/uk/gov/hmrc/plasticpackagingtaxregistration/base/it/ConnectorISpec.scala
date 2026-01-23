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

package base.it

import base.data.SubscriptionTestData
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries, Timer}
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.bootstrap.metrics.{Metrics, MetricsFilter, MetricsFilterImpl, MetricsImpl}

import scala.concurrent.ExecutionContext

class ConnectorISpec
    extends WiremockTestServer with GuiceOneAppPerSuite with DefaultAwaitTimeout
    with SubscriptionTestData {
  protected val httpClient: DefaultHttpClient = app.injector.instanceOf[DefaultHttpClient]
  protected val metrics: Metrics              = app.injector.instanceOf[Metrics]

  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier    = HeaderCarrier()

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .overrides(
        bind[MetricsFilter].to[MetricsFilterImpl],
        bind[Metrics].to[MetricsImpl]
      ).configure(overrideConfig).build()
  }

  def overrideConfig: Map[String, Any] =
    Map("microservice.services.eis.host"                   -> wireHost,
        "microservice.services.eis.port"                   -> wirePort,
        "microservice.services.nrs.host"                   -> wireHost,
        "microservice.services.nrs.port"                   -> wirePort,
        "microservice.services.tax-enrolments.host"        -> wireHost,
        "microservice.services.tax-enrolments.port"        -> wirePort,
        "microservice.services.enrolment-store-proxy.port" -> wirePort
    )

  def getTimer(name: String): Timer = {
    metrics.defaultRegistry
      .getTimers(MetricFilter.startsWith(name))
      .get(name)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def beforeEach(): Unit =
    metrics.defaultRegistry.removeMatching(MetricFilter.startsWith("ppt"))

  override protected def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

}
