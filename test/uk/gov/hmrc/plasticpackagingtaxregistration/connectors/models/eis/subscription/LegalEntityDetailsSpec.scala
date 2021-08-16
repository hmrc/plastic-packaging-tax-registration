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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnershipTypeEnum

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter

class LegalEntityDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData {
  "LegalEntityDetails" should {
    "build successfully" when {
      "subscripting an organisation" in {
        val legalEntityDetails = LegalEntityDetails(pptIncorporationDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

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
      "subscripting a general partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptGeneralPartnershipDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptGeneralPartnershipDetails.partnershipDetails.get.generalPartnershipDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptGeneralPartnershipDetails.partnershipDetails.get.generalPartnershipDetails.get.postcode
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptGeneralPartnershipDetails.organisationType.get.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipName.get
      }
      "subscripting a scottish partnership" in {
        val legalEntityDetails = LegalEntityDetails(pptScottishPartnershipDetails)
        legalEntityDetails.dateOfApplication mustBe now(UTC).format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )

        legalEntityDetails.customerIdentification1 mustBe pptScottishPartnershipDetails.partnershipDetails.get.scottishPartnershipDetails.get.sautr
        legalEntityDetails.customerIdentification2 mustBe Some(
          pptScottishPartnershipDetails.partnershipDetails.get.scottishPartnershipDetails.get.postcode
        )

        legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation

        legalEntityDetails.customerDetails.individualDetails mustBe None

        legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptScottishPartnershipDetails.organisationType.get.toString
        )
        legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipName.get
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
          LegalEntityDetails(pptIncorporationDetails.copy(incorporationDetails = None))
        }
      }

      "sole trader and sole trader details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptSoleTraderDetails.copy(soleTraderDetails = None))
        }
      }

      "partnership and partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(pptGeneralPartnershipDetails.copy(partnershipDetails = None))
        }
      }

      "partnership and unsupported partnership type" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptGeneralPartnershipDetails.copy(partnershipDetails =
              Some(
                pptGeneralPartnershipDetails.partnershipDetails.get.copy(
                  partnershipType = PartnershipTypeEnum.LIMITED_PARTNERSHIP,
                  partnershipName = None
                )
              )
            )
          )
        }
      }

      "partnership and partnership general partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptGeneralPartnershipDetails.copy(partnershipDetails =
              Some(
                pptGeneralPartnershipDetails.partnershipDetails.get.copy(generalPartnershipDetails =
                  None
                )
              )
            )
          )
        }
      }

      "partnership and partnership scottish partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptScottishPartnershipDetails.copy(partnershipDetails =
              Some(
                pptScottishPartnershipDetails.partnershipDetails.get.copy(
                  scottishPartnershipDetails = None
                )
              )
            )
          )
        }
      }
    }
  }
}
