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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{OrgType, PartnerTypeEnum}

class OrganisationDetailsSpec extends AnyWordSpec with Matchers {

  "OrganisationDetails" should {

    "mapOrganisationTypeStringsToEnums" in {
      OrganisationDetails(organisationType = Some("UkCompany"),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = false) mustBe OrgType.UK_COMPANY

      OrganisationDetails(organisationType = Some("Partnership"),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP
    }

    "read an organisationType of LIMITED COMPANY as OverseasCompanyUkBranch as fallback for gform reg's" in {
      OrganisationDetails(organisationType = Some("LIMITED COMPANY"),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = false) mustBe OrgType.OVERSEAS_COMPANY_NO_UK_BRANCH
    }

    "return partnership organisation type if a partner type enum string is found instead of an organisation type" in {
      OrganisationDetails(organisationType = Some(PartnerTypeEnum.SCOTTISH_PARTNERSHIP.toString),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP

      OrganisationDetails(organisationType =
                            Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP.toString),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = false) mustBe OrgType.PARTNERSHIP
    }

    "treat LLPs as partnerships if they are part of a group" in {
      // TODO why this is different to the above non group case?
      OrganisationDetails(organisationType =
                            Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP.toString),
                          organisationName = "My organisation"
      ).organisationTypeDisplayName(isGroup = true) mustBe OrgType.PARTNERSHIP
    }
  }

}
