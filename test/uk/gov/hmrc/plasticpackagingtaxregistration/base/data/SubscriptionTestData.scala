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

package uk.gov.hmrc.plasticpackagingtaxregistration.base.data

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription._
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.create.{
  SubscriptionFailureResponse,
  SubscriptionSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group.{
  GroupPartnershipDetails,
  GroupPartnershipSubscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatus.NOT_SUBSCRIBED
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.{
  ETMPSubscriptionStatus,
  ETMPSubscriptionStatusResponse,
  SubscriptionStatusResponse
}

trait SubscriptionTestData {

  protected val safeNumber = "123456"
  protected val idType     = "ZPPT"

  protected val subscriptionStatusResponse_HttpGet: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/subscriptions/status/" + safeNumber)

  protected val subscriptionCreate_HttpPost: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "/subscriptions/" + safeNumber)

  protected val etmpSubscriptionStatusResponse: ETMPSubscriptionStatusResponse =
    ETMPSubscriptionStatusResponse(subscriptionStatus =
                                     Some(ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND),
                                   idType = Some("ZPPT"),
                                   idValue = Some("X00000123456789")
    )

  protected val subscriptionStatusResponse: SubscriptionStatusResponse =
    SubscriptionStatusResponse(status = NOT_SUBSCRIBED, pptReference = Some("ZPPT"))

  protected val subscriptionCreateResponse: SubscriptionSuccessfulResponse =
    SubscriptionSuccessfulResponse(pptReferenceNumber = "XMPPT123456789",
                                   processingDate = now(UTC),
                                   formBundleNumber = "123456789"
    )

  protected val subscriptionCreateFailureResponse: SubscriptionFailureResponse =
    SubscriptionFailureResponse(failures =
      Seq(EISError(code = "123", reason = "error"))
    )

  protected val ukLimitedCompanySubscription: Subscription = Subscription(
    legalEntityDetails =
      LegalEntityDetails(dateOfApplication =
                           now(UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                         customerIdentification1 = "123456789",
                         customerIdentification2 = Some("1234567890"),
                         customerDetails = CustomerDetails(
                           customerType = CustomerType.Organisation,
                           organisationDetails =
                             Some(OrganisationDetails(organisationName = "Plastics Ltd"))
                         ),
                         groupSubscriptionFlag = false
      ),
    principalPlaceOfBusinessDetails =
      PrincipalPlaceOfBusinessDetails(
        addressDetails = AddressDetails(addressLine1 = "2-3 Scala Street",
                                        addressLine2 = "London",
                                        postalCode = Some("W1T 2HN"),
                                        countryCode = "GB"
        ),
        contactDetails = ContactDetails(email = "test@test.com", telephone = "02034567890")
      ),
    primaryContactDetails =
      PrimaryContactDetails(name = "Kevin Durant",
                            contactDetails =
                              ContactDetails(email = "test@test.com", telephone = "02034567890"),
                            positionInCompany = "Director"
      ),
    businessCorrespondenceDetails = BusinessCorrespondenceDetails(addressLine1 = "2-3 Scala Street",
                                                                  addressLine2 = "London",
                                                                  postalCode = Some("W1T 2HN"),
                                                                  countryCode = "GB"
    ),
    taxObligationStartDate = now(UTC).toString,
    last12MonthTotalTonnageAmt = 15000,
    declaration = Declaration(declarationBox1 = true),
    groupPartnershipSubscription = None
  )

  private val groupPartnershipDetailsRep: GroupPartnershipDetails = GroupPartnershipDetails(
    "Representative",
    "123456789",
    Some("987654321"),
    OrganisationDetails(Some("UkCompany"), "Plastic Limited"),
    IndividualDetails(None, "first", None, "last"),
    AddressDetails("line1", "line2", Some("line3"), Some("line4"), Some("postcode"), "GB"),
    ContactDetails("some@email.com", "0123-456789", None)
  )

  private val groupPartnershipDetailsMember: GroupPartnershipDetails = GroupPartnershipDetails(
    "Member",
    "member-1",
    Some("member-2"),
    OrganisationDetails(Some("UkCompany"), "Plastics Member"),
    IndividualDetails(None, "Arthur", None, "Surname"),
    AddressDetails("addressLine1",
                   "addressLine2",
                   Some("addressLine3"),
                   Some("addressLine4"),
                   Some("postcode"),
                   "GB"
    ),
    ContactDetails("member@email.com", "0987-456789", None)
  )

  protected val groupPartnershipSubscription: GroupPartnershipSubscription =
    new GroupPartnershipSubscription(representativeControl = true,
                                     allMembersControl = true,
                                     Seq(groupPartnershipDetailsRep, groupPartnershipDetailsMember)
    )

  protected val ukLimitedCompanyGroupSubscription: Subscription =
    ukLimitedCompanySubscription.copy(
      legalEntityDetails =
        ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = true),
      groupPartnershipSubscription = Some(groupPartnershipSubscription)
    )

}
