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

import play.api.libs.json.{JsPath, JsString, JsValue, Reads, Writes}

final case class PostCodeCleaner private(postcode: String)

object PostCodeCleaner {
  def apply(postcode: String) =
    new PostCodeCleaner(postcode.replaceAll(" ", ""))

  implicit val jsonWrites: Writes[PostCodeCleaner] = new Writes[PostCodeCleaner] {
    def writes(self: PostCodeCleaner): JsValue = JsString(self.postcode)
  }

  implicit val jsonReads: Reads[PostCodeCleaner] =
    (JsPath).read[String].map(PostCodeCleaner.apply)

}
