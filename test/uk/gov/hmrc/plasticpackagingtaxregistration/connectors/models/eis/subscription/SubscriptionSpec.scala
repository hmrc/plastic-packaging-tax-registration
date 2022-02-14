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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.Partner

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter

class SubscriptionSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {
  "Subscription" should {
    "build successfully" when {
      "UK Limited Company" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          ),
          isSubscriptionUpdate = false
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidIncorporationLegalEntityDetails(subscription)
      }
      "UK Limited Company with no liability weight" in {
        val subscription =
          Subscription(aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                     withPrimaryContactDetails(pptPrimaryContactDetails),
                                     withLiabilityDetails(pptLiabilityDetails.copy(weight = None))
                       ),
                       isSubscriptionUpdate = false
          )
        assertCommonDetails(subscription, None)
        mustHaveValidIncorporationLegalEntityDetails(subscription)
      }
      "Group" in {
        val subscription =
          Subscription(aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                     withPrimaryContactDetails(pptPrimaryContactDetails),
                                     withLiabilityDetails(pptLiabilityDetails),
                                     withGroupDetail(groupDetail)
                       ),
                       isSubscriptionUpdate = false
          )

        subscription.groupPartnershipSubscription.get.groupPartnershipDetails.size mustBe 2
        subscription.declaration.declarationBox1 mustBe true
        subscription.last12MonthTotalTonnageAmt mustBe 10000
        subscription.taxObligationStartDate mustBe pptLiabilityDetails.startDate.get.pretty

        mustHaveValidPrimaryContactDetails(subscription)
        mustHaveValidBusinessCorrespondenceDetails(subscription)
        mustHaveValidPrincipalPlaceOfBusinessDetails(subscription, isPartnership = false)
        mustHaveValidIncorporationLegalEntityDetails(subscription, groupFlag = true)
        mustHaveValidGroupPartnershipSubscription(subscription)
      }
      "Sole Trader" in {
        val subscription = Subscription(
          aRegistration(withOrganisationDetails(pptSoleTraderDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          ),
          isSubscriptionUpdate = false
        )
        assertCommonDetails(subscription, Some(10000))
        mustHaveValidIndividualLegalEntityDetails(subscription)
      }
      "General Partnership" in {
        val registration = aRegistration(withOrganisationDetails(pptGeneralPartnershipDetails),
                                         withLiabilityDetails(pptLiabilityDetails)
        )

        val subscription = Subscription(registration, isSubscriptionUpdate = false)

        assertCommonDetails(subscription, Some(10000), isPartnership = true)

        mustHaveValidGeneralPartnershipLegalEntityDetails(subscription)
        mustHaveValidPartners(subscription,
                              registration.organisationDetails.partnershipDetails.get.partners
        )
      }
      "Scottish Partnership" in {
        val registration = aRegistration(withOrganisationDetails(pptScottishPartnershipDetails),
                                         withLiabilityDetails(pptLiabilityDetails)
        )

        val subscription = Subscription(registration, isSubscriptionUpdate = false)

        assertCommonDetails(subscription, Some(10000), isPartnership = true)
        mustHaveValidScottishPartnershipLegalEntityDetails(subscription)
        mustHaveValidPartners(subscription,
                              registration.organisationDetails.partnershipDetails.get.partners
        )
      }
      "We have expected plastic packaging weight" in {
        val subscription =
          Subscription(aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                     withPrimaryContactDetails(pptPrimaryContactDetails),
                                     withLiabilityDetails(pptLiabilityDetailsWithExpectedWeight)
                       ),
                       isSubscriptionUpdate = false
          )
        assertCommonDetails(subscription, Some(20000))
      }
    }

    "throw an exception" when {
      "no liability start date has been provided" in {
        intercept[Exception] {
          Subscription(aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                     withPrimaryContactDetails(pptPrimaryContactDetails),
                                     withLiabilityDetails(
                                       pptLiabilityDetails.copy(startDate = None)
                                     )
                       ),
                       isSubscriptionUpdate = false
          )
        }
      }
    }
  }

  private def assertCommonDetails(
    subscription: Subscription,
    expectedPPTWeight: Option[Long],
    isPartnership: Boolean = false
  ) = {
    subscription.declaration.declarationBox1 mustBe true
    subscription.last12MonthTotalTonnageAmt mustBe expectedPPTWeight.getOrElse(0)
    subscription.taxObligationStartDate mustBe pptLiabilityDetails.startDate.get.pretty

    mustHaveValidPrimaryContactDetails(subscription, isPartnership)
    mustHaveValidBusinessCorrespondenceDetails(subscription, isPartnership)
    mustHaveValidPrincipalPlaceOfBusinessDetails(subscription, isPartnership)
  }

  private def mustHaveValidPrincipalPlaceOfBusinessDetails(
    subscription: Subscription,
    isPartnership: Boolean
  ) = {
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.getOrElse(
      ""
    )
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
    subscription.principalPlaceOfBusinessDetails.addressDetails.addressLine4 mustBe Some(
      pptBusinessAddress.townOrCity
    )
    subscription.principalPlaceOfBusinessDetails.addressDetails.postalCode mustBe pptBusinessAddress.postCode
    subscription.principalPlaceOfBusinessDetails.addressDetails.countryCode mustBe pptBusinessAddress.countryCode

    if (isPartnership) {
      val nominatedPartner = aUkCompanyPartner()
      subscription.principalPlaceOfBusinessDetails.contactDetails.email mustBe nominatedPartner.contactDetails.get.emailAddress.get
      subscription.principalPlaceOfBusinessDetails.contactDetails.telephone mustBe nominatedPartner.contactDetails.get.phoneNumber.get
    } else {
      subscription.principalPlaceOfBusinessDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
      subscription.principalPlaceOfBusinessDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
    }
    subscription.principalPlaceOfBusinessDetails.contactDetails.mobileNumber mustBe None
  }

  private def mustHaveValidBusinessCorrespondenceDetails(
    subscription: Subscription,
    isPartnership: Boolean = false
  ) =
    if (isPartnership) {
      val nominatedPartnerContactAddress = aUkCompanyPartner().contactDetails.get.address.get
      val expectedBusinessCorrespondenceDetails = BusinessCorrespondenceDetails(
        nominatedPartnerContactAddress
      )

      subscription.businessCorrespondenceDetails mustBe expectedBusinessCorrespondenceDetails
    } else {
      subscription.businessCorrespondenceDetails.addressLine1 mustBe pptPrimaryContactAddress.addressLine1
      subscription.businessCorrespondenceDetails.addressLine2 mustBe pptPrimaryContactAddress.addressLine2.getOrElse(
        ""
      )
      subscription.businessCorrespondenceDetails.addressLine3 mustBe pptPrimaryContactAddress.addressLine3
      subscription.businessCorrespondenceDetails.addressLine4 mustBe Some(
        pptPrimaryContactAddress.townOrCity
      )
      subscription.businessCorrespondenceDetails.postalCode mustBe pptPrimaryContactAddress.postCode
      subscription.businessCorrespondenceDetails.countryCode mustBe pptPrimaryContactAddress.countryCode
    }

  private def mustHaveValidIncorporationLegalEntityDetails(
    subscription: Subscription,
    groupFlag: Boolean = false
  ): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptIncorporationDetails.incorporationDetails.get.companyNumber
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptIncorporationDetails.incorporationDetails.get.ctutr
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.groupSubscriptionFlag mustBe groupFlag

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptIncorporationDetails.organisationType.get.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptIncorporationDetails.incorporationDetails.get.companyName
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidGeneralPartnershipLegalEntityDetails(subscription: Subscription): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.sautr
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptGeneralPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.postcode
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptGeneralPartnershipDetails.partnershipDetails.get.partnershipType.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptGeneralPartnershipDetails.partnershipDetails.get.partnershipName.get
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidScottishPartnershipLegalEntityDetails(
    subscription: Subscription
  ): Any = {
    subscription.legalEntityDetails.customerIdentification1 mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.sautr
    subscription.legalEntityDetails.customerIdentification2 mustBe Some(
      pptScottishPartnershipDetails.partnershipDetails.get.partnershipBusinessDetails.get.postcode
    )

    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Organisation
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationType mustBe Some(
      pptScottishPartnershipDetails.partnershipDetails.get.partnershipType.toString
    )
    subscription.legalEntityDetails.customerDetails.organisationDetails.get.organisationName mustBe pptScottishPartnershipDetails.partnershipDetails.get.partnershipName.get
    subscription.legalEntityDetails.customerDetails.individualDetails mustBe None
  }

  private def mustHaveValidIndividualLegalEntityDetails(subscription: Subscription): Any = {
    subscription.legalEntityDetails.dateOfApplication mustBe now(UTC).format(
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )
    subscription.legalEntityDetails.customerIdentification1 mustBe pptSoleTraderDetails.soleTraderDetails.get.ninoOrTrn
    subscription.legalEntityDetails.customerIdentification2 mustBe pptSoleTraderDetails.soleTraderDetails.get.sautr
    subscription.legalEntityDetails.customerDetails.customerType mustBe CustomerType.Individual

    subscription.legalEntityDetails.customerDetails.organisationDetails mustBe None

    subscription.legalEntityDetails.customerDetails.individualDetails.get.firstName mustBe pptSoleTraderDetails.soleTraderDetails.get.firstName
    subscription.legalEntityDetails.customerDetails.individualDetails.get.lastName mustBe pptSoleTraderDetails.soleTraderDetails.get.lastName
    subscription.legalEntityDetails.customerDetails.individualDetails.get.middleName mustBe None

  }

  private def mustHaveValidPrimaryContactDetails(
    subscription: Subscription,
    isPartnership: Boolean = false
  ) =
    if (isPartnership) {
      val nominatedPartner = aUkCompanyPartner()
      subscription.primaryContactDetails.name mustBe
        s"${nominatedPartner.contactDetails.get.firstName.get} ${nominatedPartner.contactDetails.get.lastName.get}"
      subscription.primaryContactDetails.positionInCompany mustBe "Nominated Partner"
      subscription.primaryContactDetails.contactDetails.email mustBe nominatedPartner.contactDetails.get.emailAddress.get
      subscription.primaryContactDetails.contactDetails.telephone mustBe nominatedPartner.contactDetails.get.phoneNumber.get
      subscription.primaryContactDetails.contactDetails.mobileNumber mustBe None
    } else {
      subscription.primaryContactDetails.name mustBe pptPrimaryContactDetails.name.get
      subscription.primaryContactDetails.positionInCompany mustBe pptPrimaryContactDetails.jobTitle.get
      subscription.primaryContactDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
      subscription.primaryContactDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
      subscription.primaryContactDetails.contactDetails.mobileNumber mustBe None
    }

  private def mustHaveValidGroupPartnershipSubscription(subscription: Subscription) = {
    subscription.groupPartnershipSubscription.get.representativeControl mustBe true
    subscription.groupPartnershipSubscription.get.allMembersControl mustBe true
    subscription.groupPartnershipSubscription.get.groupPartnershipDetails.size mustBe 2
  }

  private def mustHaveValidPartners(subscription: Subscription, partners: Seq[Partner]) = {
    subscription.groupPartnershipSubscription.get.representativeControl mustBe true
    subscription.groupPartnershipSubscription.get.allMembersControl mustBe true
    subscription.groupPartnershipSubscription.get.groupPartnershipDetails.size mustBe partners.size
    (partners zip subscription.groupPartnershipSubscription.get.groupPartnershipDetails).foreach {
      case (partner, groupPartnership) =>
        groupPartnership.relationship mustBe "Partner"
        groupPartnership.customerIdentification1 mustBe partner.customerIdentification1
        groupPartnership.customerIdentification2 mustBe partner.customerIdentification2
        groupPartnership.organisationDetails.organisationType mustBe partner.partnerType.map(
          _.toString
        )
        groupPartnership.organisationDetails.organisationName mustBe partner.name
        groupPartnership.individualDetails.firstName mustBe partner.contactDetails.flatMap(
          _.firstName
        ).get
        groupPartnership.individualDetails.lastName mustBe partner.contactDetails.flatMap(
          _.lastName
        ).get
        groupPartnership.addressDetails mustBe partner.contactDetails.map(
          cd => AddressDetails(cd.address.get)
        ).get
        groupPartnership.contactDetails.email mustBe partner.contactDetails.flatMap(
          _.emailAddress
        ).get
        groupPartnership.contactDetails.telephone mustBe partner.contactDetails.flatMap(
          _.phoneNumber
        ).get
    }
  }

}
