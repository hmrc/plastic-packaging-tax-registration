/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter

class LegalEntityDetailsSpec extends AnyWordSpec with Matchers with RegistrationTestData {
  "LegalEntityDetails" should {
    "build successfully" when {
      "subscribing an organisation" in {
        val legalEntityDetails = LegalEntityDetails(pptIncorporationDetails, false)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )
        legalEntityDetails.groupSubscriptionFlag mustBe false

        legalEntityDetails.customerIdentification1 mustBe pptIncorporationDetails.incorporationDetails.get.companyNumber
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptIncorporationDetails.incorporationDetails.get.ctutr
        )
        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptIncorporationDetails.organisationType.get.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptIncorporationDetails.incorporationDetails.get.companyName
        legalEntityDetails.customerDetails.individualDetails mustBe None
      }

      "subscribing a group" in {
        val legalEntityDetails = LegalEntityDetails(pptIncorporationDetails, true)
        legalEntityDetails.groupSubscriptionFlag mustBe true
      }

      "subscribing an individual" in {
        val legalEntityDetails = LegalEntityDetails(pptSoleTraderDetails, false)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptSoleTraderDetails.soleTraderDetails.get.ninoOrTrn
        legalEntityDetails.customerIdentification2 mustBe
          pptSoleTraderDetails.soleTraderDetails.get.sautr

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Individual

        legalEntityDetails.customerDetails.organisationDetails mustBe None

        legalEntityDetails.customerDetails.individualDetails.get.firstName mustBe pptSoleTraderDetails.soleTraderDetails.get.firstName
        legalEntityDetails.customerDetails.individualDetails.get.lastName mustBe pptSoleTraderDetails.soleTraderDetails.get.lastName
        legalEntityDetails.customerDetails.individualDetails.get.middleName mustBe None
      }
      "subscribing a general partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptGeneralPartnershipDetails, false)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptGeneralPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.postcode
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptGeneralPartnershipDetails.partnershipDetails.get.partnershipType.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipName.get
      }
      "subscribing a scottish partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptScottishPartnershipDetails, false)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptScottishPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.postcode
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptScottishPartnershipDetails.partnershipDetails.get.partnershipType.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipName.get
      }
      "subscribing a limited liability partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptLimitedLiabilityDetails, false)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptLimitedLiabilityDetails.partnershipDetails.get.partnershipBusinessDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptLimitedLiabilityDetails.partnershipDetails.get.partnershipBusinessDetails.get.companyProfile.get.companyNumber
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptLimitedLiabilityDetails.partnershipDetails.get.partnershipType.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptLimitedLiabilityDetails.partnershipDetails.get.name.get
      }
    }

    "throw an exception" when {
      "organisation type is 'None'" in {
        intercept[Exception] {
          CustomerDetails(pptIncorporationDetails.copy(organisationType = None))
        }
      }

      "corporate and incorporation details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptIncorporationDetails.copy(incorporationDetails = None), false)
        }
      }

      "sole trader and sole trader details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptSoleTraderDetails.copy(soleTraderDetails = None), false)
        }
      }

      "partnership and partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptGeneralPartnershipDetails.copy(partnershipDetails = None), false)
        }
      }

      "partnership and partnership general partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptGeneralPartnershipDetails.copy(partnershipDetails =
              Some(
                pptGeneralPartnershipDetails.partnershipDetails.get.copy(
                  partnershipBusinessDetails =
                    None
                )
              )
            ),
            false
          )
        }
      }

      "partnership and partnership scottish partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptScottishPartnershipDetails.copy(partnershipDetails =
              Some(
                pptScottishPartnershipDetails.partnershipDetails.get.copy(
                  partnershipBusinessDetails = None
                )
              )
            ),
            false
          )
        }
      }
    }
  }
}
