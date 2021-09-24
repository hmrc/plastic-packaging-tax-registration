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

package uk.gov.hmrc.plasticpackagingtaxregistration.config

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class AppConfigSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val validAppConfig: Config =
    ConfigFactory.parseString("""
        |mongodb.uri="mongodb://localhost:27017/plastic-packaging-tax"
        |mongodb.timeToLiveInSeconds=100
        |microservice.services.auth.host=localhostauth
        |microservice.services.auth.port=9988
        |microservice.services.eis.host=localhost
        |microservice.services.eis.port=8506
        |microservice.services.eis.bearerToken=test123456
        |microservice.services.nrs.host=localhost
        |microservice.services.nrs.port=8506
        |microservice.services.nrs.api-key=test-key
        |microservice.metrics.graphite.host=graphite
        |auditing.enabled=true
        |eis.environment=test
        |nrs.retries=["1s", "2s", "4s"]
    """.stripMargin)

  private val validServicesConfiguration = Configuration(validAppConfig)

  private def appConfig(conf: Configuration) = new AppConfig(conf, servicesConfig(conf))

  private def servicesConfig(conf: Configuration) = new ServicesConfig(conf)

  "AppConfig" should {
    val configService: AppConfig = appConfig(validServicesConfiguration)

    "return config as object model when configuration is valid" in {
      configService.authBaseUrl mustBe "http://localhostauth:9988"
      configService.auditingEnabled mustBe true
      configService.graphiteHost mustBe "graphite"
      configService.dbTimeToLiveInSeconds mustBe 100
    }

    "have 'subscriptionStatusUrl' defined" in {
      configService.subscriptionStatusUrl("12345678") must be(
        "http://localhost:8506/cross-regime/subscription/PPT/SAFE/12345678/status"
      )
    }

    "have 'subscriptionCreateUrl' defined" in {
      configService.subscriptionCreateUrl("12345678") must be(
        "http://localhost:8506/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=12345678"
      )
    }

    "have 'nonRepudiationSubmissionUrl' defined" in {
      configService.nonRepudiationSubmissionUrl must be("http://localhost:8506/submission")
    }

    "have 'eis environment' defined" in {
      configService.eisEnvironment must be("test")
    }

    "have 'NSR ApiKey' defined" in {
      configService.nonRepudiationApiKey must be("test-key")
    }

    "have 'nrs retries' defined" in {
      configService.nrsRetries must be(
        Seq(FiniteDuration(1, TimeUnit.SECONDS),
            FiniteDuration(2, TimeUnit.SECONDS),
            FiniteDuration(4, TimeUnit.SECONDS)
        )
      )
    }

  }
}
