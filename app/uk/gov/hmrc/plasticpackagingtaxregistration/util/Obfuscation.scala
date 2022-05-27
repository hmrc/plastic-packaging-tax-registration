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

package uk.gov.hmrc.plasticpackagingtaxregistration.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.slf4j.LoggerFactory

object Obfuscation {
  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)

  def obfuscate(value: String): String = {
    val result = MessageDigest.getInstance("MD5")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map(c => "%02X".format(c)).mkString

    logger.debug(s"obfuscated [$value] as [$result]")
    result
  }

  implicit class StringObfuscationOps(inner: String) {
    def obfuscated: String = Obfuscation.obfuscate(inner)
  }

}
