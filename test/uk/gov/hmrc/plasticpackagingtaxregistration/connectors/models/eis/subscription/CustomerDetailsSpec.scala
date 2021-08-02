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

class CustomerDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData {
  "CustomerDetails" should {
    "build successfully" when {
      "subscripting an organisation" in {
        val customerDetails = CustomerDetails(pptOrganisationDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptOrganisationDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe
          pptOrganisationDetails.incorporationDetails.get.companyName

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
        val customerDetails = CustomerDetails(pptPartnershipDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.individualDetails mustBe None

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptPartnershipDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe "TODO"
      }

      "subscripting a scottishPartnership" in {
        val customerDetails = CustomerDetails(pptScottishPartnershipDetails)
        customerDetails.customerType mustBe CustomerType.Organisation

        customerDetails.individualDetails mustBe None

        customerDetails.organisationDetails.get.organisationType mustBe Some(
          pptScottishPartnershipDetails.organisationType.get.toString
        )
        customerDetails.organisationDetails.get.organisationName mustBe "TODO"
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
          CustomerDetails(pptOrganisationDetails.copy(incorporationDetails = None))
        }
      }

      "sole trader and sole trader details are missing" in {
        intercept[Exception] {
          CustomerDetails(pptSoleTraderDetails.copy(soleTraderDetails = None))
        }
      }

      "partnership and partnership details are missing" in {
        intercept[Exception] {
          CustomerDetails(pptPartnershipDetails.copy(partnershipDetails = None))
        }
      }

      "partnership and partnership general partnership details are missing" in {
        intercept[Exception] {
          LegalEntityDetails(
            pptPartnershipDetails.copy(partnershipDetails =
              Some(
                pptPartnershipDetails.partnershipDetails.get.copy(generalPartnershipDetails = None)
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
