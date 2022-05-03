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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, not}
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.{
  RegistrationTestData,
  SubscriptionTestData
}
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  ChangeOfCircumstance,
  ChangeOfCircumstanceDetails,
  Subscription
}

import java.time.{ZoneOffset, ZonedDateTime}

class RegistrationSpec
    extends AnyWordSpec with RegistrationTestData with RegistrationBuilder
    with SubscriptionTestData {

  override implicit def toPostcode(value: String): PostCodeWithoutSpaces =
    PostCodeWithoutSpaces(value)

  "Registration" should {

    "convert from subscription to registration and then to subscription " in {
      val amendedGroupSubscription =
        ukLimitedCompanySubscription.copy(
          legalEntityDetails =
            ukLimitedCompanySubscription.legalEntityDetails.copy(
              groupSubscriptionFlag = true,
              regWithoutIDFlag = Some(true) // Always true for group subscriptions
            ),
          groupPartnershipSubscription =
            Some(groupPartnershipSubscription),
          changeOfCircumstanceDetails =
            Some(
              ChangeOfCircumstanceDetails(changeOfCircumstance =
                ChangeOfCircumstance.UPDATE_TO_DETAILS.toString
              )
            ),
          processingDate = Some(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate.toString)
        )
      val rehydratedRegistration = Registration(amendedGroupSubscription)

      val updatedSubscription = Subscription(rehydratedRegistration, isSubscriptionUpdate = true)

      updatedSubscription mustBe amendedGroupSubscription

    }

    "convert from UK company subscription" in {

      val ukCompanyRegistration =
        aRegistration(withOrganisationDetails(pptIncorporationDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from overseas company with UK branch subscription" in {

      val ukCompanyRegistration =
        aRegistration(
          withOrganisationDetails(
            pptIncorporationDetails.copy(organisationType =
              Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH)
            )
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from registered society subscription" in {

      val ukCompanyRegistration =
        aRegistration(
          withOrganisationDetails(
            pptIncorporationDetails.copy(organisationType =
              Some(OrgType.REGISTERED_SOCIETY)
            )
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from sole trader subscription" in {

      val soleTraderRegistration =
        aRegistration(withOrganisationDetails(pptSoleTraderDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(soleTraderRegistration)

    }

    "convert from general partner subscription" in {

      val generalPartnershipRegistration =
        aRegistration(withOrganisationDetails(pptGeneralPartnershipDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(generalPartnershipRegistration)

    }

    "convert partnership details when mapping from a subscription" in {
      val partnershipDetails = pptGeneralPartnershipDetails.partnershipDetails.map(
        _.copy(partnershipType = PartnerTypeEnum.SCOTTISH_PARTNERSHIP)
      )
      val partnershipRegistration =
        aRegistration(
          withOrganisationDetails(
            pptGeneralPartnershipDetails.copy(partnershipDetails = partnershipDetails)
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

      partnershipRegistration.organisationDetails.organisationType mustBe Some(OrgType.PARTNERSHIP)
      partnershipRegistration.organisationDetails.partnershipDetails.map(
        _.partnershipType
      ) mustBe Some(PartnerTypeEnum.SCOTTISH_PARTNERSHIP)
      val existingSubscription = Subscription(partnershipRegistration, false)

      val rehydratedRegistration = Registration(existingSubscription)

      rehydratedRegistration.isPartnership mustBe true
      rehydratedRegistration.organisationDetails.partnershipDetails.nonEmpty mustBe true
      val rehydratedPartnershipDetails =
        rehydratedRegistration.organisationDetails.partnershipDetails.get

      rehydratedPartnershipDetails.partnershipType mustBe PartnerTypeEnum.SCOTTISH_PARTNERSHIP
      rehydratedPartnershipDetails.partners.size mustBe partnershipRegistration.organisationDetails.partnershipDetails.get.partners.size
      rehydratedPartnershipDetails.partners.map(
        _.partnerType
      ) mustBe partnershipRegistration.organisationDetails.partnershipDetails.get.partners.map(
        _.partnerType
      )

      // Check the first partner in detail
      val nominatedPartner =
        partnershipRegistration.organisationDetails.partnershipDetails.get.partners.head
      val rehydratedNominatedPartner = rehydratedPartnershipDetails.partners.head

      // Partner type
      rehydratedNominatedPartner.partnerType mustBe Some(PartnerTypeEnum.UK_COMPANY)
      // Partner contact details

      rehydratedNominatedPartner.contactDetails.flatMap(_.jobTitle) mustBe Some("Director")
      rehydratedNominatedPartner.contactDetails mustBe nominatedPartner.contactDetails

      // As we are storing job title on subscription primary contact details, we can only keep
      // one value; we should apply this to the nominated partner; all other partners have no job title.
      val rehydratedNonNominatedPartners =
        rehydratedPartnershipDetails.partners.find(_.id != rehydratedNominatedPartner.id)
      rehydratedNonNominatedPartners.forall(
        _.contactDetails.flatMap(_.jobTitle).isEmpty
      ) mustBe true

      // Incorporated entities will have populated the incorporationDetails field.
      rehydratedNominatedPartner.incorporationDetails.nonEmpty mustBe true
      rehydratedNominatedPartner.incorporationDetails.map(
        _.companyName
      ) mustBe nominatedPartner.incorporationDetails.map(_.companyName)
      rehydratedNominatedPartner.incorporationDetails.map(
        _.companyNumber
      ) mustBe nominatedPartner.incorporationDetails.map(_.companyNumber)
      rehydratedNominatedPartner.incorporationDetails.map(
        _.ctutr
      ) mustBe nominatedPartner.incorporationDetails.map(_.ctutr)

      // IncorporationAddressDetails does not appear to be mapped to Subscription; or used for anything in the backend
      nominatedPartner.incorporationDetails.get.companyAddress mustBe IncorporationAddressDetails()           // Subscription does not map anything into these fields
      rehydratedNominatedPartner.incorporationDetails.get.companyAddress mustBe IncorporationAddressDetails() // Subscription does not map anything into these fields

      //  Incorporated entities will not have populated the soleTraderDetails or partnerPartnershipDetails fields.
      rehydratedNominatedPartner.soleTraderDetails mustBe None
      rehydratedNominatedPartner.partnerPartnershipDetails mustBe None

      // Examine a rehydrated sole trader
      // We should assert all of the sole trade details fields here
      val soleTraderPartner =
        partnershipRegistration.organisationDetails.partnershipDetails.get.partners.find(
          _.partnerType.contains(PartnerTypeEnum.SOLE_TRADER)
        ).get
      val rehydratedSoleTraderPartner =
        rehydratedRegistration.organisationDetails.partnershipDetails.get.partners.find(
          _.partnerType.contains(PartnerTypeEnum.SOLE_TRADER)
        ).get

      rehydratedSoleTraderPartner.soleTraderDetails.nonEmpty mustBe true
      rehydratedSoleTraderPartner.soleTraderDetails.map(
        _.firstName
      ) mustBe soleTraderPartner.soleTraderDetails.map(_.firstName)
      rehydratedSoleTraderPartner.soleTraderDetails.map(
        _.lastName
      ) mustBe soleTraderPartner.soleTraderDetails.map(_.lastName)

      // There is no way to map date of birth on individualDetails so it cannot be round tripped
      soleTraderPartner.soleTraderDetails.flatMap(_.dateOfBirth).nonEmpty mustBe true
      rehydratedSoleTraderPartner.soleTraderDetails.flatMap(_.dateOfBirth).isEmpty mustBe true

      rehydratedSoleTraderPartner.soleTraderDetails.map(
        _.ninoOrTrn
      ) mustBe soleTraderPartner.soleTraderDetails.map(_.ninoOrTrn)
      rehydratedSoleTraderPartner.soleTraderDetails.map(
        _.sautr
      ) mustBe soleTraderPartner.soleTraderDetails.map(_.sautr)

      // Examine a partner type partner to investigate the mapping of partnerPartnershipDetails.
      val partnershipPartner =
        partnershipRegistration.organisationDetails.partnershipDetails.get.partners.find(
          _.partnerType.contains(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP)
        ).get
      val rehydratedPartnershipPartnerPartner =
        rehydratedRegistration.organisationDetails.partnershipDetails.get.partners.find(
          _.partnerType.contains(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP)
        ).get

      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.nonEmpty mustBe true
      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipBusinessDetails
      ).nonEmpty mustBe true

      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipName
      ) mustBe Some("Partners in Plastic")

      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipBusinessDetails
      ).map(_.postcode) mustBe
        partnershipPartner.partnerPartnershipDetails.flatMap(_.partnershipBusinessDetails).map(
          _.postcode
        )
      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipBusinessDetails
      ).map(_.sautr) mustBe
        partnershipPartner.partnerPartnershipDetails.flatMap(_.partnershipBusinessDetails).map(
          _.sautr
        )

      // partnershipBusinessDetails / companyProfile
      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipBusinessDetails
      ).flatMap(_.companyProfile).nonEmpty mustBe true
      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.flatMap(
        _.partnershipBusinessDetails
      ).flatMap(_.companyProfile).map(_.companyName) mustBe
        partnershipPartner.partnerPartnershipDetails.flatMap(_.partnershipBusinessDetails).flatMap(
          _.companyProfile
        ).map(_.companyName)

      // None of these partners have RegistrationDetails set because we think this field is just
      // an artifact of top level entire classes been reused to represent partner details.
      // Therefore none of these partners will have RegistrationDetails set.
      // Explicit gets to confirm we're asserting in the right fields for each partner class
      rehydratedNominatedPartner.incorporationDetails.get.registration mustBe None
      rehydratedPartnershipPartnerPartner.partnerPartnershipDetails.get.partnershipBusinessDetails.get.registration mustBe None
      rehydratedSoleTraderPartner.soleTraderDetails.get.registration mustBe None
    }

    "convert from UK company group subscription" in {

      val ukCompanyGroupRegistration =
        aRegistration(withOrganisationDetails(pptIncorporationDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails),
                      withGroupDetail(groupDetail)
        )

      assertConversion(ukCompanyGroupRegistration)

    }

    def assertConversion(registration: Registration) = {

      val existingSubscription   = Subscription(registration, isSubscriptionUpdate = false)
      val rehydratedRegistration = Registration(existingSubscription)
      val updatedSubscription    = Subscription(rehydratedRegistration, isSubscriptionUpdate = false)

      updatedSubscription mustBe existingSubscription
    }

    "propagate regWithoutId flags through a subscription variation as expected" in {
      val groupSubscription = ukLimitedCompanySubscription.copy(
        legalEntityDetails = ukLimitedCompanySubscription.legalEntityDetails.copy(
          groupSubscriptionFlag = true,
          regWithoutIDFlag = Some(true) // Always true for group subscriptions
        ),
        groupPartnershipSubscription = Some(groupPartnershipSubscription)
      )

      groupSubscription.groupPartnershipSubscription.get.groupPartnershipDetails.head.regWithoutIDFlag mustBe Some(
        false
      )
      groupSubscription.groupPartnershipSubscription.get.groupPartnershipDetails(
        1
      ).regWithoutIDFlag mustBe Some(true)

      val rehydratedRegistration = Registration(groupSubscription)

      val variationSubscription = Subscription(rehydratedRegistration, isSubscriptionUpdate = true)

      variationSubscription.legalEntityDetails.regWithoutIDFlag mustBe
        groupSubscription.legalEntityDetails.regWithoutIDFlag
      variationSubscription.groupPartnershipSubscription.get.groupPartnershipDetails.head.regWithoutIDFlag mustBe
        groupSubscription.groupPartnershipSubscription.get.groupPartnershipDetails.head.regWithoutIDFlag
      variationSubscription.groupPartnershipSubscription.get.groupPartnershipDetails(
        1
      ).regWithoutIDFlag mustBe
        groupSubscription.groupPartnershipSubscription.get.groupPartnershipDetails(
          1
        ).regWithoutIDFlag
    }

    "contain post code without spaces" when {
      "registration is a partnership" in {
        val reg = aRegistration(
          withOrganisationDetails(
            pptGeneralPartnershipDetails.copy(incorporationDetails =
              Some(
                IncorporationDetails(companyNumber = "1234567890",
                                     companyName = "Plastic Limited",
                                     ctutr = "987654321",
                                     companyAddress = IncorporationAddressDetails(postal_code =
                                       Some("  VR3 6JK ")
                                     ),
                                     registration = Some(registrationDetails)
                )
              )
            )
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

        reg.primaryContactDetails.address.get.postCode.get.postcode.postcode must not contain ' '
        assertPostCodeInOrgDetails(reg.organisationDetails)
        reg.primaryContactDetails.address.get.postCode.get.postcode must not contain ' '
      }

      "registration is a group membership" in {
        val reg =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withGroupDetail(groupDetail)
          )

        reg.groupDetail.get.members.foreach { o =>
          o.contactDetails.get.address.get.postCode.get.postcode must not contain ' '
          o.addressDetails.postCode.get.postcode must not contain ' '
        }
      }
    }
  }

  private def assertPostCodeInOrgDetails(orgDetails: OrganisationDetails) = {
    orgDetails.businessRegisteredAddress.get.postCode.get.postcode must not contain ' '
    orgDetails.partnershipDetails.get.partnershipBusinessDetails.get.postcode.postcode must not contain ' '
    orgDetails.partnershipDetails.get.partners.foreach(
      _.contactDetails.get.address.get.postCode.get.postcode must not contain ' '
    )
    orgDetails.incorporationDetails.get.companyAddress.postal_code.get.postcode must not contain ' '
  }

}
