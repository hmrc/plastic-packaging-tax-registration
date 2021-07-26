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
        val legalEntityDetails = LegalEntityDetails(pptSoleTraderDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptSoleTraderDetails.soleTraderDetails.get.nino
        legalEntityDetails.customerIdentification2 mustBe
          pptSoleTraderDetails.soleTraderDetails.get.sautr

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Individual

        legalEntityDetails.customerDetails.organisationDetails mustBe None

        legalEntityDetails.customerDetails.individualDetails.get.firstName mustBe pptSoleTraderDetails.soleTraderDetails.get.firstName
        legalEntityDetails.customerDetails.individualDetails.get.lastName mustBe pptSoleTraderDetails.soleTraderDetails.get.lastName
        legalEntityDetails.customerDetails.individualDetails.get.middleName mustBe None
      }
      "subscripting a partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptPartnershipDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptPartnershipDetails.partnershipDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptPartnershipDetails.partnershipDetails.get.postcode
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptPartnershipDetails.organisationType.get.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe "TODO"
      }
    }

    "throw an exception" when {
      "organisation type is 'None'" in {
        intercept[Exception] {
          CustomerDetails(pptOrganisationDetails.copy(organisationType = None))
        }
      }

      "corporate and incorporation details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptOrganisationDetails.copy(incorporationDetails = None))
        }
      }

      "sole trader and sole trader details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptSoleTraderDetails.copy(soleTraderDetails = None))
        }
      }

      "partnership and partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptPartnershipDetails.copy(partnershipDetails = None))
        }
      }
    }
  }
}
