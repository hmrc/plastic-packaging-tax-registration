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
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import reactivemongo.api.ReadConcern
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONLong
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.models._

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationRepositoryItSpec
    extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach
    with IntegrationPatience with RegistrationBuilder with DefaultAwaitTimeout {

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
      .count(selector = None,
             limit = Some(0),
             skip = 0,
             hint = None,
             readConcern = ReadConcern.Local
      )
      .futureValue
      .toInt

  private def givenARegistrationExists(registrations: Registration*): Unit =
    repository.collection.insert(ordered = true).many(registrations).futureValue

  "MongoRepository" should {
    "update existing index on first use" in {
      ensureExpiryTtlOnIndex(injector.instanceOf[AppConfig].dbTimeToLiveInSeconds)

      val repository = new GuiceApplicationBuilder().configure(
        Map("mongodb.timeToLiveInSeconds" -> 33)
      ).build().injector.instanceOf[RegistrationRepository]
      await(repository.create(aRegistration()))

      ensureExpiryTtlOnIndex(33)
    }

    "create new ttl index when none exist" in {
      await(this.repository.collection.indexesManager.dropAll())

      val repository = new GuiceApplicationBuilder().configure(
        Map("mongodb.timeToLiveInSeconds" -> 99)
      ).build().injector.instanceOf[RegistrationRepository]
      await(repository.create(aRegistration()))

      ensureExpiryTtlOnIndex(99)
    }

    def ensureExpiryTtlOnIndex(ttlSeconds: Int): Unit =
      eventually(timeout(Span(5, Seconds))) {
        {
          val indexes = await(repository.collection.indexesManager.list())
          val expiryTtl = indexes.find(index => index.eventualName == "ttlIndex").map(
            getExpireAfterSecondsOptionOf
          )
          expiryTtl.get mustBe ttlSeconds
        }
      }

    def getExpireAfterSecondsOptionOf(idx: Index): Long =
      idx.options.getAs[BSONLong]("expireAfterSeconds").getOrElse(BSONLong(0)).as[Long]
  }

  "Create" should {
    "persist the registration" in {
      val registration = aRegistration()
      repository.create(registration).futureValue mustBe registration

      collectionSize mustBe 1
    }

    "update lastModifiedDateTime field" in {
      val registration = aRegistration()

      await(repository.create(registration))
      val newRegistration = await(repository.findByRegistrationId(registration.id))

      newRegistration.get.lastModifiedDateTime must not be None
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
                                  Address(addressLine1 = "addressLine1",
                                          townOrCity = "Town",
                                          postCode = "PostCode"
                                  )
                                )
          )
        ),
        withOrganisationDetails(
          OrganisationDetails(isBasedInUk = Some(true),
                              organisationType = Some(OrgType.UK_COMPANY),
                              businessRegisteredAddress =
                                Some(
                                  Address(addressLine1 = "addressLine1",
                                          townOrCity = "Town",
                                          postCode = "PostCode"
                                  )
                                )
          )
        )
      )
      givenARegistrationExists(registration)

      val updatedRegistration = await(repository.update(registration)).get

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

    "update lastModifiedDateTime on each registration update" in {
      val registration = aRegistration()
      await(repository.create(registration))
      val initialLastModifiedDateTime =
        await(repository.findByRegistrationId(registration.id)).get.lastModifiedDateTime.get

      val updatedRegistration = repository.update(registration).futureValue.get

      updatedRegistration.lastModifiedDateTime.get.isAfter(initialLastModifiedDateTime) mustBe true
    }

    "do nothing for missing registration" in {
      val registration = aRegistration(withTimestamp(DateTime.now(DateTimeZone.UTC)))

      repository.update(registration).futureValue mustBe None

      collectionSize mustBe 0

      getTimer("ppt.registration.mongo.update").getCount mustBe 1
    }
  }

  "Find by ID" should {
    "return the persisted registration" when {
      "one exists with ID" in {
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
