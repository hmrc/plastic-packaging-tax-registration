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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData

class PartnerSpec extends AnyWordSpec with RegistrationTestData {
  "Partner" should {
    "return first customer identifier" when {
      "sole trader" in {
        val soleTraderPartner = aSoleTraderPartner()
        soleTraderPartner.customerIdentification1 mustBe soleTraderPartner.soleTraderDetails.get.ninoOrTrn
      }
      "uk company" in {
        val ukCompanyPartner = aUkCompanyPartner()
        ukCompanyPartner.customerIdentification1 mustBe ukCompanyPartner.incorporationDetails.get.companyNumber
      }
      "partnership" in {
        val partnershipPartner = aPartnershipPartner()
        partnershipPartner.customerIdentification1 mustBe partnershipPartner.partnerPartnershipDetails.get.partnershipBusinessDetails.get.sautr
      }
    }
    "return second customer identifier" when {
      "sole trader" in {
        val soleTraderPartner = aSoleTraderPartner()
        soleTraderPartner.customerIdentification2 mustBe soleTraderPartner.soleTraderDetails.get.sautr
      }
      "uk company" in {
        val ukCompanyPartner = aUkCompanyPartner()
        ukCompanyPartner.customerIdentification2 mustBe Some(
          ukCompanyPartner.incorporationDetails.get.ctutr
        )
      }
      "partnership" in {
        val partnershipPartner = aPartnershipPartner()
        partnershipPartner.customerIdentification2 mustBe Some(
          partnershipPartner.partnerPartnershipDetails.get.partnershipBusinessDetails.get.postcode
        )
      }
    }
    "return name" when {
      "sole trader" in {
        val soleTraderPartner = aSoleTraderPartner()
        soleTraderPartner.name mustBe s"${soleTraderPartner.soleTraderDetails.get.firstName} ${soleTraderPartner.soleTraderDetails.get.lastName}"
      }
      "uk company" in {
        val ukCompanyPartner = aUkCompanyPartner()
        ukCompanyPartner.name mustBe ukCompanyPartner.incorporationDetails.get.companyName
      }
      "partnership" in {
        val partnershipPartner = aPartnershipPartner()
        partnershipPartner.name mustBe partnershipPartner.partnerPartnershipDetails.get.name.get
      }
    }
    "throw IllegalStateExceptions" when {
      "key data is absent" when {
        "accessing type specifics for uk company" in {
          val ukCompanyPartner = aUkCompanyPartner().copy(incorporationDetails = None)
          intercept[IllegalStateException] {
            ukCompanyPartner.customerIdentification1
          }
          intercept[IllegalStateException] {
            ukCompanyPartner.customerIdentification2
          }
          intercept[IllegalStateException] {
            ukCompanyPartner.name
          }
        }
        "accessing type specifics for sole trader" in {
          val soleTraderPartner = aSoleTraderPartner().copy(soleTraderDetails = None)
          intercept[IllegalStateException] {
            soleTraderPartner.customerIdentification1
          }
          intercept[IllegalStateException] {
            soleTraderPartner.customerIdentification2
          }
          intercept[IllegalStateException] {
            soleTraderPartner.name
          }
        }
        "accessing type specifics for partnership" in {
          val partnershipPartner = aPartnershipPartner().copy(partnerPartnershipDetails = None)
          intercept[IllegalStateException] {
            partnershipPartner.customerIdentification1
          }
          intercept[IllegalStateException] {
            partnershipPartner.customerIdentification2
          }
          intercept[IllegalStateException] {
            partnershipPartner.name
          }
        }
      }
    }
  }
}
