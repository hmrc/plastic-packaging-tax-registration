package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{OrgType, PartnerTypeEnum}

class OrganisationDetailsSpec  extends AnyWordSpec with Matchers{

  "OrganisationDetails" should {

    "mapOrganisationTypeStringsToEnums" in {
      OrganisationDetails(organisationType = Some("UkCompany"), organisationName = "My organisation").
        organisationTypeDisplayName(isGroup = false) mustBe OrgType.UK_COMPANY

      OrganisationDetails(organisationType = Some("Partnership"), organisationName = "My organisation").
        organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP
    }

    "return partnership organisation type if a partner type enum string is found instead of an organisation type" in {
      OrganisationDetails(organisationType = Some(PartnerTypeEnum.SCOTTISH_PARTNERSHIP.toString), organisationName = "My organisation").
        organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP

      OrganisationDetails(organisationType = Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP.toString), organisationName = "My organisation").
        organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP
    }

    "treat LLPs as partnerships if they are part of a group" in {
        // TODO why this is different to the above non group case?
      OrganisationDetails(organisationType = Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP.toString), organisationName = "My organisation").
        organisationTypeDisplayName(isGroup = true) mustBe OrgType.PARTNERSHIP
    }
  }

}
