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
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{Partner, Registration}

class PrimaryContactDetailsSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {

  private val registration =
    Registration(id = "123", primaryContactDetails = pptPrimaryContactDetails)

  "PrimaryContactDetails" when {
    "populating from primary contact details (non-partnership registration)" should {
      "build successfully" in {
        val primaryContactDetails = PrimaryContactDetails(registration)
        primaryContactDetails.name mustBe pptPrimaryContactDetails.name.get
        primaryContactDetails.positionInCompany mustBe pptPrimaryContactDetails.jobTitle.get
        primaryContactDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
        primaryContactDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
        primaryContactDetails.contactDetails.mobileNumber mustBe None
      }

      "throw IllegalStateException" when {
        "primary contact name is absent" in {
          intercept[IllegalStateException] {
            PrimaryContactDetails(
              registration.copy(primaryContactDetails =
                registration.primaryContactDetails.copy(name = None)
              )
            )
          }
        }

        "primary contact job title is absent" in {
          intercept[IllegalStateException] {
            PrimaryContactDetails(
              registration.copy(primaryContactDetails =
                registration.primaryContactDetails.copy(jobTitle = None)
              )
            )
          }
        }

      }
    }

    "populating from partnership registration" should {
      "build successfully" in {
        val partnershipRegistration =
          aRegistration(withLiabilityDetails(pptLiabilityDetails),
                        withOrganisationDetails(pptGeneralPartnershipDetails)
          )

        val primaryContactDetails = PrimaryContactDetails(partnershipRegistration)

        val nominatedPartner = pptGeneralPartnershipDetails.partnershipDetails.get.partners.head

        primaryContactDetails.name mustBe
          s"${nominatedPartner.contactDetails.get.firstName.get} ${nominatedPartner.contactDetails.get.lastName.get}"
        primaryContactDetails.positionInCompany mustBe "Nominated Partner"
        primaryContactDetails.contactDetails.email mustBe nominatedPartner.contactDetails.get.emailAddress.get
        primaryContactDetails.contactDetails.telephone mustBe nominatedPartner.contactDetails.get.phoneNumber.get
        primaryContactDetails.contactDetails.mobileNumber mustBe None
      }

      "throw IllegalStateException" when {
        "nominated partner is absent" in {
          val partnershipRegistrationWithNoPartners =
            aRegistration(withLiabilityDetails(pptLiabilityDetails),
                          withOrganisationDetails(
                            pptGeneralPartnershipDetails.copy(partnershipDetails =
                              pptGeneralPartnershipDetails.partnershipDetails.map(
                                _.copy(partners = Seq())
                              )
                            )
                          )
            )

          intercept[IllegalStateException] {
            PrimaryContactDetails(partnershipRegistrationWithNoPartners)
          }
        }
        "nominated partner contact detail is absent" in {
          val partnershipRegistrationWithMissingPartnerNames =
            aRegistration(withLiabilityDetails(pptLiabilityDetails),
                          withOrganisationDetails(pptGeneralPartnershipDetails),
                          withPartnerModifications(partner => partner.copy(contactDetails = None))
            )

          intercept[IllegalStateException] {
            PrimaryContactDetails(partnershipRegistrationWithMissingPartnerNames)
          }
        }
        "nominated partner contact name is absent" in {
          val partnershipRegistrationWithMissingPartnerNames =
            aRegistration(withLiabilityDetails(pptLiabilityDetails),
                          withOrganisationDetails(pptGeneralPartnershipDetails),
                          withPartnerModifications(
                            partner =>
                              partner.copy(contactDetails =
                                partner.contactDetails.map(
                                  _.copy(firstName = None, lastName = None)
                                )
                              )
                          )
            )

          intercept[IllegalStateException] {
            PrimaryContactDetails(partnershipRegistrationWithMissingPartnerNames)
          }
        }

      }

    }

  }

}
