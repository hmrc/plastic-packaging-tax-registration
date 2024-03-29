/*
 * Copyright 2024 HM Revenue & Customs
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

package base.data

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import base.AuthTestSupport
import models.eis.EISError
import models.eis.subscription._
import models.eis.subscription.create.{
  EISSubscriptionFailureResponse,
  SubscriptionSuccessfulResponse
}
import models.eis.subscription.group.GroupPartnershipDetails.Relationship
import models.eis.subscription.group.{GroupPartnershipDetails, GroupPartnershipSubscription}
import models.eis.subscriptionStatus.SubscriptionStatus.NOT_SUBSCRIBED
import models.eis.subscriptionStatus.{
  ETMPSubscriptionStatus,
  ETMPSubscriptionStatusResponse,
  SubscriptionStatusResponse
}
import models.{OrgType, PostCodeWithoutSpaces}

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter
import scala.language.implicitConversions

trait SubscriptionTestData extends AuthTestSupport {

  implicit def toPostcode(value: String): PostCodeWithoutSpaces = PostCodeWithoutSpaces(value)

  protected val safeNumber   = "123456"
  protected val idType       = "ZPPT"
  protected val pptReference = userEnrolledPptReference

  protected val subscriptionStatusResponse_HttpGet: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/subscriptions/status/" + safeNumber)

  protected val subscriptionResponse_HttpGet: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/subscriptions/" + pptReference)

  protected val subscriptionResponse_HttpPut: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("PUT", "/subscriptions/" + pptReference)

  protected val subscriptionCreate_HttpPost: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "/subscriptions/" + safeNumber)

  protected val subscriptionDeregister_HttpPUT: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("PUT", "/subscriptions/deregister/" + pptReference)

  protected val etmpSubscriptionStatusResponse: ETMPSubscriptionStatusResponse =
    ETMPSubscriptionStatusResponse(subscriptionStatus =
                                     Some(ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND),
                                   idType = Some("ZPPT"),
                                   idValue = Some("X00000123456789")
    )

  protected val subscriptionStatusResponse: SubscriptionStatusResponse =
    SubscriptionStatusResponse(status = NOT_SUBSCRIBED, pptReference = Some("ZPPT"))

  protected val subscriptionSuccessfulResponse: SubscriptionSuccessfulResponse =
    SubscriptionSuccessfulResponse(pptReferenceNumber = "XMPPT123456789",
                                   processingDate = now(UTC),
                                   formBundleNumber = "123456789"
    )

  protected val subscriptionCreateFailureResponse: EISSubscriptionFailureResponse =
    EISSubscriptionFailureResponse(failures =
      Seq(EISError(code = "123", reason = "error"))
    )

  protected val ukLimitedCompanySubscription: Subscription = Subscription(
    legalEntityDetails =
      LegalEntityDetails(dateOfApplication =
                           now(UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                         customerIdentification1 = "123456789",
                         customerIdentification2 = Some("1234567890"),
                         customerDetails = CustomerDetails(customerType = CustomerType.Organisation,
                                                           organisationDetails =
                                                             Some(
                                                               OrganisationDetails(
                                                                 organisationType = Some(
                                                                   OrgType.UK_COMPANY.toString
                                                                 ),
                                                                 organisationName = "Plastics Ltd"
                                                               )
                                                             )
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
    taxObligationStartDate = now(UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    last12MonthTotalTonnageAmt = 15000,
    declaration = Declaration(declarationBox1 = true),
    groupPartnershipSubscription = None
  )

  protected val ukLimitedCompanySubscriptionInvalid: Subscription = Subscription(
    legalEntityDetails =
      LegalEntityDetails(dateOfApplication =
                           "foo",
                         customerIdentification1 = "123456789",
                         customerIdentification2 = Some("1234567890"),
                         customerDetails = CustomerDetails(customerType = CustomerType.Organisation,
                                                           organisationDetails =
                                                             Some(
                                                               OrganisationDetails(
                                                                 organisationType = Some(
                                                                   OrgType.UK_COMPANY.toString
                                                                 ),
                                                                 organisationName = "Plastics Ltd"
                                                               )
                                                             )
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
    taxObligationStartDate = now(UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    last12MonthTotalTonnageAmt = 15000,
    declaration = Declaration(declarationBox1 = true),
    groupPartnershipSubscription = None
  )

  protected val groupPartnershipDetailsRep: GroupPartnershipDetails = GroupPartnershipDetails(
    Relationship.Representative,
    "123456789",
    Some("1234567890"),
    OrganisationDetails(Some("UkCompany"), "Plastics Ltd"),
    IndividualDetails(None, "Kevin", None, "Durant"),
    AddressDetails("2-3 Scala Street", "London", None, None, Some("W1T 2HN"), "GB"),
    ContactDetails("test@test.com", "02034567890", None),
    regWithoutIDFlag = Some(false)
  )

  protected val groupPartnershipDetailsMember: GroupPartnershipDetails = GroupPartnershipDetails(
    Relationship.Member,
    "member1",
    Some("member2"),
    OrganisationDetails(Some("UkCompany"), "Plastics Member"),
    IndividualDetails(None, "Arthur", None, "Surname"),
    AddressDetails("addressLine1",
                   "addressLine2",
                   Some("addressLine3"),
                   Some("addressLine4"),
                   Some("ZZ11ZZ"),
                   "GB"
    ),
    ContactDetails("member@email.com", "0987-456789", None),
    regWithoutIDFlag = Some(true)
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
