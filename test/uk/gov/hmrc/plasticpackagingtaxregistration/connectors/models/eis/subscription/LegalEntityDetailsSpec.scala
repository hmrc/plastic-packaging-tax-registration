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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.{
  RegistrationTestData,
  SubscriptionTestData
}

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter

class LegalEntityDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData {
  "LegalEntityDetails" should {
    "build successfully" when {
      "subscripting an organisation" in {
        val legalEntityDetails = LegalEntityDetails(pptOrganisationDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptOrganisationDetails.incorporationDetails.get.companyNumber
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptOrganisationDetails.incorporationDetails.get.ctutr
        )
        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptOrganisationDetails.organisationType.get.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptOrganisationDetails.incorporationDetails.get.companyName
        legalEntityDetails.customerDetails.individualDetails mustBe None
      }
      "subscripting an individual" in {
        val legalEntityDetails = LegalEntityDetails(pptIndividualDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptIndividualDetails.soleTraderDetails.get.nino
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptIndividualDetails.soleTraderDetails.get.dateOfBirth
        )
        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Individual

        legalEntityDetails.customerDetails.organisationDetails mustBe None

        legalEntityDetails.customerDetails.individualDetails.get.firstName mustBe pptIndividualDetails.soleTraderDetails.get.firstName
        legalEntityDetails.customerDetails.individualDetails.get.lastName mustBe pptIndividualDetails.soleTraderDetails.get.lastName
        legalEntityDetails.customerDetails.individualDetails.get.middleName mustBe None
      }
    }

    "throw an exception" when {
      "incorporation details are missing from the organisation" in {
        intercept[Exception] {
          LegalEntityDetails(pptOrganisationDetails.copy(incorporationDetails = None))
        }
      }

      "incorporation details are missing from the individual" in {
        intercept[Exception] {
          LegalEntityDetails(pptIndividualDetails.copy(soleTraderDetails = None))
        }
      }
    }
  }
}
