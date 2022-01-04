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

class CustomerDetailsSpec extends AnyWordSpec with Matchers with RegistrationTestData {
  "CustomerDetails" should {
    "build successfully" when {
      "subscripting an organisation" in {
        val customerDetails = CustomerDetails(pptIncorporationDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptIncorporationDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe
          pptIncorporationDetails.incorporationDetails.get.companyName

        customerDetails.individualDetails mustBe None
      }

      "subscripting an individual" in {
        val customerDetails = CustomerDetails(pptSoleTraderDetails)
        customerDetails.customerType mustBe CustomerType.Individual

        customerDetails.organisationDetails mustBe None

        customerDetails.individualDetails.get.firstName mustBe pptSoleTraderDetails.soleTraderDetails.get.firstName
        customerDetails.individualDetails.get.lastName mustBe pptSoleTraderDetails.soleTraderDetails.get.lastName
        customerDetails.individualDetails.get.middleName mustBe None
      }

      "subscripting a generalPartnership" in {
        val customerDetails = CustomerDetails(pptGeneralPartnershipDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.individualDetails mustBe None

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptGeneralPartnershipDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipName.get
      }

      "subscripting a scottishPartnership" in {
        val customerDetails = CustomerDetails(pptScottishPartnershipDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.individualDetails mustBe None

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptScottishPartnershipDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipName.get
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
          CustomerDetails(pptIncorporationDetails.copy(incorporationDetails = None))
        }
      }

      "sole trader and sole trader details are missing" in {
        intercept[Exception] {
          CustomerDetails(pptSoleTraderDetails.copy(soleTraderDetails = None))
        }
      }

      "partnership and partnership details are missing" in {
        intercept[Exception] {
          CustomerDetails(pptGeneralPartnershipDetails.copy(partnershipDetails = None))
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
            isGroup = false
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
            isGroup = false
          )
        }
      }
    }
  }
}
