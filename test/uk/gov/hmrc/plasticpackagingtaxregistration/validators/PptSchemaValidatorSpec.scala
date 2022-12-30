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

package validators

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import base.data.SubscriptionTestData
import models.eis.subscription.group.GroupPartnershipSubscription

class PptSchemaValidatorSpec extends AnyWordSpec with Matchers with SubscriptionTestData {

  "validate" when {
    "given a valid create" should {
      "return success" in {
        PptSchemaValidator.subscriptionValidator.validate(
          ukLimitedCompanySubscription
        ).isRight mustBe true
      }
    }

    "given an invalid create" should {
      "return failure" in {
        PptSchemaValidator.subscriptionValidator.validate(
          ukLimitedCompanySubscriptionInvalid
        ).isRight mustBe false
      }
    }

    "creating a subscription with groupSubscriptionFlag as true and partnershipSubscriptionFlag as true" in {
      val data = ukLimitedCompanySubscription.copy(
        legalEntityDetails =
          ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = true,
                                                               partnershipSubscriptionFlag = true
          ),
        groupPartnershipSubscription = Some(
          GroupPartnershipSubscription(representativeControl = true,
                                       allMembersControl = true,
                                       Seq(groupPartnershipDetailsRep,
                                           groupPartnershipDetailsMember
                                       ).map(_.copy(regWithoutIDFlag = None))
          )
        )
      )

      PptSchemaValidator.subscriptionValidator.validate(data).isRight mustBe true
    }

    "creating a subscription with groupSubscriptionFlag as true partnershipSubscriptionFlag as false" in {
      val data = ukLimitedCompanySubscription.copy(
        legalEntityDetails =
          ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = true,
                                                               partnershipSubscriptionFlag = false
          ),
        groupPartnershipSubscription = Some(
          GroupPartnershipSubscription(representativeControl = true,
                                       allMembersControl = true,
                                       Seq(groupPartnershipDetailsRep,
                                           groupPartnershipDetailsMember
                                       ).map(_.copy(regWithoutIDFlag = None))
          )
        )
      )

      PptSchemaValidator.subscriptionValidator.validate(data).isRight mustBe true
    }

    "creating a subscription with groupSubscriptionFlag as false partnershipSubscriptionFlag as true" in {
      val data = ukLimitedCompanySubscription.copy(
        legalEntityDetails =
          ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = false,
                                                               partnershipSubscriptionFlag = true
          ),
        groupPartnershipSubscription = Some(
          GroupPartnershipSubscription(representativeControl = true,
                                       allMembersControl = true,
                                       Seq(groupPartnershipDetailsRep,
                                           groupPartnershipDetailsMember
                                       ).map(_.copy(regWithoutIDFlag = None))
          )
        )
      )

      PptSchemaValidator.subscriptionValidator.validate(data).isRight mustBe true
    }

    "creating a subscription with groupSubscriptionFlag  as false and partnershipSubscriptionFlag as false" in {
      val data = ukLimitedCompanySubscription.copy(
        legalEntityDetails =
          ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = false,
                                                               partnershipSubscriptionFlag = false
          ),
        groupPartnershipSubscription = None
      )

      PptSchemaValidator.subscriptionValidator.validate(data).isRight mustBe true
    }
  }
}
