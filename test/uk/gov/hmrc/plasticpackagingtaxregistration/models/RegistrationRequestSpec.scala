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

package models

import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}
import builders.RegistrationRequestBuilder

class RegistrationRequestSpec
    extends AnyWordSpec with must.Matchers with RegistrationRequestBuilder {
  "can parse from a request with UserHeaders" in {
    val jsonWithUserHeaders = """{
                |  "dateOfRegistration" : "2022-05-27",
                |  "incorpJourneyId" : "f368e653-790a-4a95-af62-4132f0ffd433",
                |  "liabilityDetails" : { },
                |  "primaryContactDetails" : { },
                |  "organisationDetails" : { },
                |  "metaData" : {
                |    "registrationReviewed" : false,
                |    "registrationCompleted" : false,
                |    "verifiedEmails" : [ ]
                |  },
                |  "userHeaders" : {
                |    "header" : "value"
                |  }
                |}""".stripMargin
    Json.parse(jsonWithUserHeaders).as[RegistrationRequest].userHeaders mustBe (Map(
      "header" -> "value"
    ))
  }

  "can parse from a request without UserHeaders" in {
    val jsonWithoutUserHeaders = """{
                                |  "dateOfRegistration" : "2022-05-27",
                                |  "incorpJourneyId" : "f368e653-790a-4a95-af62-4132f0ffd433",
                                |  "liabilityDetails" : { },
                                |  "primaryContactDetails" : { },
                                |  "organisationDetails" : { },
                                |  "metaData" : {
                                |    "registrationReviewed" : false,
                                |    "registrationCompleted" : false,
                                |    "verifiedEmails" : [ ]
                                |  }
                                |}""".stripMargin
    Json.parse(jsonWithoutUserHeaders).as[RegistrationRequest].userHeaders mustBe (Map.empty)
  }

  "excludes field when serialised to Json with no userHeaders" in {
    val registrationRequest = aRegistrationRequest(withNoUserHeaders())
    Json.toJson(registrationRequest).as[JsObject]
      .fields.map(_._1) must not(contain("userHeaders"))
  }

  "Can roundtrip through Json" in {
    val registrationRequest = aRegistrationRequest(withUserHeaders(Map("Foo" -> "Bar")))

    Json.toJson(registrationRequest).as[RegistrationRequest] mustBe registrationRequest
  }
}
