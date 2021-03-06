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
        val customerDetails = CustomerDetails(pptIndividualDetails)
        customerDetails.customerType mustBe CustomerType.Individual

        customerDetails.organisationDetails mustBe None

        customerDetails.individualDetails.get.firstName mustBe pptIndividualDetails.soleTraderDetails.get.firstName
        customerDetails.individualDetails.get.lastName mustBe pptIndividualDetails.soleTraderDetails.get.lastName
        customerDetails.individualDetails.get.middleName mustBe None

      }
    }

    "throw an exception" when {
      "organisation type is 'None'" in {
        intercept[Exception] {
          CustomerDetails(pptOrganisationDetails.copy(organisationType = None))
        }
      }

      "incorporation details are missing from the organisation" in {
        intercept[Exception] {
          CustomerDetails(pptOrganisationDetails.copy(incorporationDetails = None))
        }
      }

      "incorporation details are missing from the individual" in {
        intercept[Exception] {
          CustomerDetails(pptIndividualDetails.copy(soleTraderDetails = None))
        }
      }
    }
  }
}
