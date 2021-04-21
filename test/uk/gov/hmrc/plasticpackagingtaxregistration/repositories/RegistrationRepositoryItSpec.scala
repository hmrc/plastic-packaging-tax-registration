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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.ReadConcern
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{Address, FullName, PrimaryContactDetails, Registration}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationRepositoryItSpec
    extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach with IntegrationPatience
    with RegistrationBuilder {

  private val injector = {
    SharedMetricRegistries.clear()
    GuiceApplicationBuilder().injector()
  }

  private val repository = injector.instanceOf[RegistrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
    repository.removeAll().futureValue
  }

  private def collectionSize: Int =
    repository.collection
      .count(selector = None, limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
      .futureValue
      .toInt

  private def givenARegistrationExists(registrations: Registration*): Unit =
    repository.collection.insert(ordered = true).many(registrations).futureValue

  "Create" should {
    "persist the registration" in {
      val registration = aRegistration()
      repository.create(registration).futureValue mustBe registration

      collectionSize mustBe 1
    }
  }

  "Update" should {
    "update the registration" in {
      val registration = aRegistration(
        withPrimaryContactDetails(
          PrimaryContactDetails(Some(FullName(firstName = "FirstName", lastName = "LastName")),
                                jobTitle = Some("CEO"),
                                email = Some("test@test.com"),
                                phoneNumber = Some("1234567890"),
                                address = Some(
                                  Address(addressLine1 = "addressLine1", townOrCity = "Town", postCode = "PostCode")
                                )
          )
        ),
        withBusinessAddress(Address(addressLine1 = "addressLine1", townOrCity = "Town", postCode = "PostCode"))
      )
      givenARegistrationExists(registration)

      repository.update(registration).futureValue mustBe Some(registration)

      // this indicates that a timer has started and has been stopped
      getTimer("ppt.registration.mongo.update").getCount mustBe 1

      collectionSize mustBe 1
    }

    "do nothing for missing registration" in {
      val registration = aRegistration()

      repository.update(registration).futureValue mustBe None

      collectionSize mustBe 0

      getTimer("ppt.registration.mongo.update").getCount mustBe 1
    }
  }

  "Find by ID" should {
    "return the persisted registration" when {
      "one exists with ID" in {
        val registration = aRegistration(
          withBusinessAddress(Address(addressLine1 = "addressLine1", townOrCity = "Town", postCode = "PostCode"))
        )
        givenARegistrationExists(registration)

        repository.findByRegistrationId(registration.id).futureValue mustBe Some(registration)

        // this indicates that a timer has started and has been stopped
        getTimer("ppt.registration.mongo.find").getCount mustBe 1
      }
    }

    "return None" when {
      "none exist with id" in {
        val registration1 = aRegistration(withId("some-other-id"))
        val registration2 = aRegistration()
        givenARegistrationExists(registration1, registration2)

        repository.findByRegistrationId("non-existing-id").futureValue mustBe None

        getTimer("ppt.registration.mongo.find").getCount mustBe 1
      }
    }
  }

  "Delete" should {
    "remove the registration" in {
      val registration = aRegistration()
      givenARegistrationExists(registration)

      repository.delete(registration).futureValue

      collectionSize mustBe 0
    }

    "maintain other registrations" when {
      "they have a different ID" in {
        val registration1 = aRegistration()
        val registration2 = aRegistration(withId("id1"))
        val registration3 = aRegistration(withId("id2"))
        givenARegistrationExists(registration2, registration3)

        repository.delete(registration1).futureValue

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
