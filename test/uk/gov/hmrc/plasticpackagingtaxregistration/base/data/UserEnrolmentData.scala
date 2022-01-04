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

package uk.gov.hmrc.plasticpackagingtaxregistration.base.data

import java.time.LocalDate

import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.KeyValue
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.UserEnrolmentRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy.{
  Enrolment,
  QueryKnownFactsResponse
}

trait UserEnrolmentData {

  val knownPptReference: String        = "XMPPT000123456"
  val knownRegistrationDate: LocalDate = LocalDate.parse("2021-10-01")
  val knownPostcode: String            = "AA11AA"

  def queryKnownFactsResponse(pptReference: String = knownPptReference): QueryKnownFactsResponse =
    QueryKnownFactsResponse(
      "HMRC-PPT-ORG",
      Seq(
        Enrolment(identifiers = Seq(KeyValue("EtmpRegistrationNumber", pptReference)),
                  verifiers = Seq.empty // Not used by service ATM
        )
      )
    )

  val userEnrolmentRequest: UserEnrolmentRequest =
    UserEnrolmentRequest(knownPptReference, knownRegistrationDate, Some(knownPostcode))

  def pptEnrolment(pptReference: String = knownPptReference): Enrolment =
    Enrolment(identifiers = Seq(KeyValue("EtmpRegistrationNumber", pptReference)),
              verifiers =
                Seq(KeyValue("PPTRegistrationDate", "20211018"),
                    KeyValue("BusinessPostCode", "AA11AA")
                )
    )

  val irsaEnrolment: Enrolment = Enrolment(
    identifiers = Seq(KeyValue("UTR", "1234567890")),
    verifiers =
      Seq(KeyValue("NINO", "AB112233D"), KeyValue("Postcode", "SW1A 2AA"))
  )

}
