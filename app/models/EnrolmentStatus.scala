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

package models

import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Reads}

sealed trait EnrolmentStatus

object EnrolmentStatus {

  case object Success extends EnrolmentStatus {
    val jsonName = "SUCCEEDED"
  }

  case object Failure extends EnrolmentStatus {
    val jsonName = "ERROR"
  }

  case object Enrolled extends EnrolmentStatus {
    val jsonName = "Enrolled"
  }

  case object EnrolmentError extends EnrolmentStatus {
    val jsonName = "EnrolmentError"
  }

  case object AuthRefreshed extends EnrolmentStatus {
    val jsonName = "AuthRefreshed"
  }

  implicit object EnrolmentStatusJsonReads extends Reads[EnrolmentStatus] {

    override def reads(json: JsValue): JsResult[EnrolmentStatus] =
      json.validate[String].flatMap {
        case Success.jsonName => JsSuccess(Success)
        case Failure.jsonName | Enrolled.jsonName | EnrolmentError.jsonName |
            AuthRefreshed.jsonName =>
          JsSuccess(Failure)
        case s => JsError(s"$s can not be read as EnrolmentStatus")
      }

  }

}
