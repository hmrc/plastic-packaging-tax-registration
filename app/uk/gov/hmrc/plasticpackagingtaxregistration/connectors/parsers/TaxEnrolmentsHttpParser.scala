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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers

import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object TaxEnrolmentsHttpParser {
  type TaxEnrolmentsResponse = Either[FailedTaxEnrolment, SuccessfulTaxEnrolment.type]

  implicit object TaxEnrolmentsHttpReads extends HttpReads[TaxEnrolmentsResponse] {

    override def read(method: String, url: String, response: HttpResponse): TaxEnrolmentsResponse =
      response.status match {
        case NO_CONTENT => Right(SuccessfulTaxEnrolment)
        case status     => Left(FailedTaxEnrolment(status))
      }

  }

  case object SuccessfulTaxEnrolment

  case class FailedTaxEnrolment(status: Int)

}
