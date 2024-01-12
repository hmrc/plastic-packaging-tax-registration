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

package util

import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpec

class ObfuscationSpec extends AnyWordSpec with must.Matchers {
  "obfuscating as string leads to another string" in {
    Obfuscation.obfuscate("the quick brown fox jumps over the lazy dog") must not(
      be("the quick brown fox jumps over the lazy dog")
    )
  }

  "obfuscating the same string twice leads to the same result" in {
    val a = Obfuscation.obfuscate("the quick brown fox jumps over the lazy dog")
    val b = Obfuscation.obfuscate("the quick brown fox jumps over the lazy dog")

    a mustBe b
  }

  "obfuscating different strings leads to different results" in {
    val a = Obfuscation.obfuscate("the quick brown fox jumps over the lazy dog")
    val b = Obfuscation.obfuscate("tdlikajdkljad akl jsdkhsa kdh")

    a must not(be(b))
  }
}
