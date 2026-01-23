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

package base.unit

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import base.AuthTestSupport
import base.data.{NrsTestData, RegistrationTestData, SubscriptionTestData}
import connectors.{
  EnrolmentStoreProxyConnector,
  NonRepudiationConnector,
  SubscriptionsConnector,
  TaxEnrolmentsConnector
}
import models.PostCodeWithoutSpaces
import repositories.RegistrationRepository
import services.nrs.NonRepudiationService

import scala.concurrent.ExecutionContext

trait ControllerSpec
    extends AnyWordSpecLike with MockitoSugar with Matchers with GuiceOneAppPerSuite
    with AuthTestSupport with BeforeAndAfterEach with DefaultAwaitTimeout with MockConnectors
    with RegistrationTestData with SubscriptionTestData with NrsTestData {

  SharedMetricRegistries.clear()

  override implicit def toPostcode(value: String): PostCodeWithoutSpaces =
    PostCodeWithoutSpaces(value)

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  protected implicit val hc: HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization(testAuthToken)))

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector),
               bind[SubscriptionsConnector].to(mockSubscriptionsConnector),
               bind[NonRepudiationConnector].to(mockNonRepudiationConnector),
               bind[RegistrationRepository].to(mockRepository),
               bind[NonRepudiationService].to(mockNonRepudiationService),
               bind[TaxEnrolmentsConnector].to(mockTaxEnrolmentsConnector),
               bind[EnrolmentStoreProxyConnector].to(mockEnrolmentStoreProxyConnector)
    )
    .build()

  protected val mockNonRepudiationService: NonRepudiationService = mock[NonRepudiationService]
  protected val mockRepository: RegistrationRepository           = mock[RegistrationRepository]

}
