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

package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}

class PostCodeWithoutSpacesSpec extends PlaySpec {

  "apply" should {
    "return postcode without space " in {
      PostCodeWithoutSpaces("GH76HJ").postcode mustBe "GH76HJ"
    }

    "remove spaces from a postcode" in {
      PostCodeWithoutSpaces("GH7  6HJ").postcode mustBe "GH76HJ"
    }

    "trim white spaces" in {
      PostCodeWithoutSpaces(" GH7  6HJ ").postcode mustBe "GH76HJ"
    }
  }

  "jsonWrite" should {
    "return postcode as json string" in {
      Json.toJson(PostCodeWithoutSpaces("GH76HJ")) mustBe JsString("GH76HJ")
    }
  }

  "jsonReads" should {
    "read postcode as string" in {
      Json.parse(JsString("GH76HJ").toString()).as[
        PostCodeWithoutSpaces
      ] mustBe PostCodeWithoutSpaces("GH76HJ")
    }
  }
}
