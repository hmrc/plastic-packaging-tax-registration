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

package uk.gov.hmrc.plasticpackagingtaxregistration.repositories

import com.codahale.metrics.{MetricFilter, SharedMetricRegistries, Timer}
import com.kenshoo.play.metrics.Metrics
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.models._

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationRepositorySpec
    extends AnyWordSpec with DefaultPlayMongoRepositorySupport[Registration] with Matchers
    with ScalaFutures with DefaultAwaitTimeout with BeforeAndAfterEach with MockitoSugar
    with RegistrationBuilder {

  private val injector = {
    SharedMetricRegistries.clear()
    GuiceApplicationBuilder().injector()
  }

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.dbTimeToLiveInSeconds) thenReturn 1

  private val metrics = injector.instanceOf[Metrics]

  override lazy val repository =
    new RegistrationRepositoryImpl(mongoComponent, mockAppConfig, metrics)

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  private def collectionSize: Int = count().futureValue.toInt

  private def givenARegistrationExists(registrations: Registration*): Unit =
    registrations.foreach(registration => await(insert(registration)))

  "findByRegistrationId" should {

    "return Registration" when {

      "record with id exists" in {

        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(isBasedInUk = Some(true),
                                organisationType = Some(OrgType.UK_COMPANY),
                                businessRegisteredAddress = Some(
                                  Address(addressLine1 = "addressLine1",
                                          townOrCity = "Town",
                                          postCode = "PostCode"
                                  )
                                )
            )
          )
        )
        givenARegistrationExists(registration)

        collectionSize mustBe 1

        val result = await(repository.findByRegistrationId(registration.id))

        result mustBe Some(registration)

        // this indicates that a timer has started and has been stopped
        getTimer("ppt.registration.mongo.find").getCount mustBe 1

      }
    }

    "return None" when {

      "no record exists" in {

        val registration1 = aRegistration(withId("some-other-id"))
        val registration2 = aRegistration()
        givenARegistrationExists(registration1, registration2)

        val result = await(repository.findByRegistrationId("does-not-exist"))

        result mustBe None

        getTimer("ppt.registration.mongo.find").getCount mustBe 1
      }
    }
  }

  "create" should {

    "persist the registration with lastModifiedDateTime" in {

      collectionSize mustBe 0
      val registration = aRegistration()

      registration.lastModifiedDateTime mustBe None

      await(repository.create(registration))

      collectionSize mustBe 1

      val saved = await(repository.findByRegistrationId(registration.id)).get

      saved.lastModifiedDateTime must not be None

      getTimer("ppt.registration.mongo.create").getCount mustBe 1
    }

  }

  "update" should {

    "update an existing registration" in {

      insert(aRegistration(withId("123"))).futureValue

      val saved = await(repository.findByRegistrationId("123")).get
      saved.lastModifiedDateTime mustBe None

      val registration = aRegistration(withId("123"),
                                       withPrimaryContactDetails(
                                         PrimaryContactDetails(name = Some("FirstName LastName"),
                                                               jobTitle = Some("CEO"),
                                                               email = Some("test@test.com"),
                                                               phoneNumber = Some("1234567890"),
                                                               address = Some(
                                                                 Address(addressLine1 =
                                                                           "addressLine1",
                                                                         townOrCity = "Town",
                                                                         postCode = "PostCode"
                                                                 )
                                                               )
                                         )
                                       ),
                                       withOrganisationDetails(
                                         OrganisationDetails(isBasedInUk = Some(true),
                                                             organisationType =
                                                               Some(OrgType.UK_COMPANY),
                                                             businessRegisteredAddress =
                                                               Some(
                                                                 Address(addressLine1 =
                                                                           "addressLine1",
                                                                         townOrCity = "Town",
                                                                         postCode = "PostCode"
                                                                 )
                                                               )
                                         )
                                       )
      )

      await(repository.update(registration))

      val updatedRegistration = await(repository.findByRegistrationId("123")).get
      updatedRegistration.lastModifiedDateTime must not be None

      updatedRegistration.id mustBe registration.id
      updatedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      updatedRegistration.liabilityDetails mustBe registration.liabilityDetails
      updatedRegistration.primaryContactDetails mustBe registration.primaryContactDetails
      updatedRegistration.organisationDetails mustBe registration.organisationDetails
      updatedRegistration.metaData mustBe registration.metaData

      // this indicates that a timer has started and has been stopped
      getTimer("ppt.registration.mongo.update").getCount mustBe 1

      collectionSize mustBe 1
    }

    "do nothing for missing registration" in {
      val registration = aRegistration()

      await(repository.update(registration)) mustBe None

      collectionSize mustBe 0

      getTimer("ppt.registration.mongo.update").getCount mustBe 1
    }

    "update lastModifiedDateTime on each registration update" in {
      val registration = aRegistration()
      await(repository.create(registration))
      val initialLastModifiedDateTime =
        await(repository.findByRegistrationId(registration.id)).get.lastModifiedDateTime.get

      val updatedRegistration = await(repository.update(registration)).get

      updatedRegistration.lastModifiedDateTime.get.isAfter(initialLastModifiedDateTime) mustBe true
    }
  }

  "delete" should {

    "remove the registration" in {
      val registration = aRegistration()
      givenARegistrationExists(registration)

      repository.delete(registration.id).futureValue

      collectionSize mustBe 0

      getTimer("ppt.registration.mongo.delete").getCount mustBe 1
    }

    "maintain other registration" when {
      "they have a different ID" in {
        val registration1 = aRegistration()
        val registration2 = aRegistration(withId("id1"))
        val registration3 = aRegistration(withId("id2"))
        givenARegistrationExists(registration1, registration2, registration3)

        repository.delete(registration1.id).futureValue

        collectionSize mustBe 2
      }
    }
  }

  private def getTimer(name: String): Timer =
    SharedMetricRegistries
      .getOrCreate("plastic-packaging-tax-registration")
      .getTimers(MetricFilter.startsWith(name))
      .get(name)

}
