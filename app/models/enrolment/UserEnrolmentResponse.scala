/*
 * Copyright 2023 HM Revenue & Customs
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

package models.enrolment

import play.api.libs.json._
import models.enrolment.EnrolmentFailedCode.EnrolmentFailedCode

trait UserEnrolmentResponse {
  val pptReference: String
}

case class UserEnrolmentSuccessResponse(pptReference: String) extends UserEnrolmentResponse

object UserEnrolmentSuccessResponse {

  implicit val format: OFormat[UserEnrolmentSuccessResponse] =
    Json.format[UserEnrolmentSuccessResponse]

}

object EnrolmentFailedCode extends Enumeration {
  type EnrolmentFailedCode = Value
  val Failed: Value               = Value
  val VerificationMissing: Value  = Value
  val VerificationFailed: Value   = Value
  val GroupEnrolled: Value        = Value
  val GroupEnrolmentFailed: Value = Value
  val UserEnrolmentFailed: Value  = Value

  implicit val format: Format[EnrolmentFailedCode] =
    Format(Reads.enumNameReads(EnrolmentFailedCode), Writes.enumNameWrites)

}

case class UserEnrolmentFailedResponse(pptReference: String, failureCode: EnrolmentFailedCode)
    extends UserEnrolmentResponse

object UserEnrolmentFailedResponse {

  implicit val format: OFormat[UserEnrolmentFailedResponse] =
    Json.format[UserEnrolmentFailedResponse]

}
