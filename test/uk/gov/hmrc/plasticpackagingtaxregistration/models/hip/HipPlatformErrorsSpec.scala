/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtaxregistration.models.hip

import models.hip.HipPlatformErrors.HipErrorWrapper
import org.scalatest.Inspectors.forAll
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class HipPlatformErrorsSpec extends AnyWordSpec {

  val failuresArrayErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "Type of Failure",
      |        "reason": "Reason for Failure"
      |      },
      |      {
      |        "type": "Another type of Failure",
      |        "reason": "More reasons"
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin)

  val objErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HoD",
      |  "response": {
      |    "error": {
      |      "code": "400",
      |      "logID": "00000000000000000000000000000000",
      |      "message": "String"
      |    }
      |  }
      |}
      |""".stripMargin)


  "HipPlatformErrors" should {
    "be read and written correctly" in {
      forAll(Seq(failuresArrayErr, objErr)) { jsValue =>
        assert (Json.toJson(jsValue.as[HipErrorWrapper]) === jsValue)
      }
    }
  }

}
