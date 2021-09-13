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
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter

class SubscriptionSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData
    with RegistrationBuilder {
  "Subscription" should {
    "build successfully" when {
      "UK Limited Company" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidIncorporationLegalEntityDetails(subscription)
      }
      "UK Limited Company with no liability weight" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails.copy(weight = None))
          )
        )
        assertCommonDetails(subscription, None)
        mustHaveValidIncorporationLegalEntityDetails(subscription)
      }
      "Sole Trader" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptSoleTraderDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidIndividualLegalEntityDetails(subscription)
      }
      "General Partnership" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptGeneralPartnershipDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidGeneralPartnershipLegalEntityDetails(subscription)
      }
      "Scottish Partnership" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptScottishPartnershipDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidScottishPartnershipLegalEntityDetails(subscription)
      }
    }

    "throw an exception" when {
      "no liability start date has been provided" in {
        intercept[Exception] {
          Subscription(
            aRegistration(withOrganisationDetails(pptIncorporationDetails),
                          withPrimaryContactDetails(pptPrimaryContactDetails),
                          withLiabilityDetails(pptLiabilityDetails.copy(startDate = None))
            )
          )
        }
      }
    }
  }

  private def assertCommonDetails(
    subscription: Subscription,
    expectedPPTWeight: Option[Integer]
  ) = {
    subscription.groupPartnershipSubscription mustBe None
    subscription.declaration.declarationBox1 mustBe true
    subscription.last12MonthTotalTonnageAmt mustBe expectedPPTWeight
    subscription.taxObligationStartDate mustBe pptLiabilityDetails.startDate.get.pretty

    mustHaveValidPrimaryContactDetails(subscription)
    mustHaveValidBusinessCorrespondenceDetails(subscription)
    mustHaveValidPrincipalPlaceOfBusinessDetails(subscription)
  }

  private def mustHaveValidPrincipalPlaceOfBusinessDetails(subscription: Subscription) = {
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.get
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine4 mustBe Some(
      pptBusinessAddress.townOrCity
    )
    subscription.principalPlaceOfBusinessDetails.addressDetails.postalCode mustBe Some(
      pptBusinessAddress.postCode
    )
    subscription.principalPlaceOfBusinessDetails.addressDetails.countryCode mustBe pptBusinessAddress.country.get

    subscription.principalPlaceOfBusinessDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
    subscription.principalPlaceOfBusinessDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
    subscription.principalPlaceOfBusinessDetails.contactDetails.mobileNumber mustBe None
  }

  private def mustHaveValidBusinessCorrespondenceDetails(subscription: Subscription) = {
    subscription.businessCorrespondenceDetails.addressLine1 mustBe pptPrimaryContactAddress.addressLine1
    subscription.businessCorrespondenceDetails.addressLine2 mustBe pptPrimaryContactAddress.addressLine2.get
    subscription.businessCorrespondenceDetails.addressLine3 mustBe pptPrimaryContactAddress.addressLine3
    subscription.businessCorrespondenceDetails.addressLine4 mustBe Some(
      pptPrimaryContactAddress.townOrCity
    )
    subscription.businessCorrespondenceDetails.postalCode mustBe Some(
      pptPrimaryContactAddress.postCode
    )
    subscription.businessCorrespondenceDetails.countryCode mustBe pptPrimaryContactAddress.country.get
  }

  private def mustHaveValidIncorporationLegalEntityDetails(subscription: Subscription): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptIncorporationDetails.incorporationDetails.get.companyNumber
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptIncorporationDetails.incorporationDetails.get.ctutr
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptIncorporationDetails.organisationType.get.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptIncorporationDetails.incorporationDetails.get.companyName
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidGeneralPartnershipLegalEntityDetails(subscription: Subscription): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptGeneralPartnershipDetails.partnershipDetails.get.generalPartnershipDetails.get.sautr
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptGeneralPartnershipDetails.partnershipDetails.get.generalPartnershipDetails.get.postcode
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptGeneralPartnershipDetails.organisationType.get.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipName.get
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidScottishPartnershipLegalEntityDetails(
    subscription: Subscription
  ): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptScottishPartnershipDetails.partnershipDetails.get.scottishPartnershipDetails.get.sautr
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptScottishPartnershipDetails.partnershipDetails.get.scottishPartnershipDetails.get.postcode
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptScottishPartnershipDetails.organisationType.get.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipName.get
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidIndividualLegalEntityDetails(subscription: Subscription): Any = {
    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )
    subscription.legalEntityDetails.customerIdentification1 mustBe pptSoleTraderDetails.soleTraderDetails.get.nino
    subscription.legalEntityDetails.customerIdentification2 mustBe pptSoleTraderDetails.soleTraderDetails.get.sautr
    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Individual

    subscription.legalEntityDetails.customerDetails.organisationDetails mustBe None

    subscription.legalEntityDetails.customerDetails.individualDetails.get.firstName mustBe pptSoleTraderDetails.soleTraderDetails.get.firstName
    subscription.legalEntityDetails.customerDetails.individualDetails.get.lastName mustBe pptSoleTraderDetails.soleTraderDetails.get.lastName
    subscription.legalEntityDetails.customerDetails.individualDetails.get.middleName mustBe None

  }

  private def mustHaveValidPrimaryContactDetails(subscription: Subscription) = {
    subscription.primaryContactDetails.name mustBe pptPrimaryContactDetails.fullName.get.fullName
    subscription.primaryContactDetails.positionInCompany mustBe pptPrimaryContactDetails.jobTitle.get
    subscription.primaryContactDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
    subscription.primaryContactDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
    subscription.primaryContactDetails.contactDetails.mobileNumber mustBe None
  }

}
