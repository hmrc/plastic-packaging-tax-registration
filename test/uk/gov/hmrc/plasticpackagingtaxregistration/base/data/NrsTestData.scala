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

package base.data

import org.joda.time
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import models.nrs.IdentityData
import services.nrs.NonRepudiationService.NonRepudiationIdentityRetrievals

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

trait NrsTestData {

  val testAffinityGroup: AffinityGroup     = AffinityGroup.Organisation
  val testProviderId: String               = "testProviderID"
  val testProviderType: String             = "GovernmentGateway"
  val testCredentials: Credentials         = Credentials(testProviderId, testProviderType)
  val testInternalid                       = "INT-123-456-789"
  val testExternalId                       = "testExternalId"
  val testAgentCode                        = "testAgentCode"
  val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
  val testSautr                            = "testSautr"
  val testNino                             = "NB686868C"

  val testDate: LocalDate         = LocalDate.of(2017, 1, 1)
  val testDateTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

  val testAuthName: Name =
    uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))

  val testAuthDateOfBirth: org.joda.time.LocalDate = org.joda.time.LocalDate.now()
  val testEmail: String                            = "testEmail"
  val testPPTReference: String                     = "XMPPT123456789"
  val testAuthToken: String                        = "testAuthToken"
  val testUserHeaders: Map[String, String]         = Map("testKey" -> "testValue")
  val testSearchKeys: Map[String, String]          = Map("pptReference" -> testPPTReference)

  val testAgentInformation: AgentInformation =
    AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))

  val testGroupIdentifier                  = "testGroupIdentifier"
  val testCredentialRole: User.type        = User
  val testMdtpInformation: MdtpInformation = MdtpInformation("testDeviceId", "testSessionId")

  val testItmpName: ItmpName =
    ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))

  val testItmpDateOfBirth: time.LocalDate = org.joda.time.LocalDate.now()

  val testItmpAddress: ItmpAddress =
    ItmpAddress(Some("testLine1"), None, None, None, None, Some("testPostcode"), None, None)

  val testCredentialStrength: String = CredentialStrength.strong

  val testNonRepudiationIdentityData: IdentityData = IdentityData(Some(testInternalid),
                                                                  Some(testExternalId),
                                                                  Some(testAgentCode),
                                                                  Some(testCredentials),
                                                                  testConfidenceLevel,
                                                                  Some(testNino),
                                                                  Some(testSautr),
                                                                  Some(testAuthName),
                                                                  Some(testEmail),
                                                                  testAgentInformation,
                                                                  Some(testGroupIdentifier),
                                                                  Some(testCredentialRole),
                                                                  Some(testMdtpInformation),
                                                                  Some(testItmpName),
                                                                  Some(testItmpAddress),
                                                                  Some(testAffinityGroup),
                                                                  Some(testCredentialStrength)
  )

  val identityJson: JsValue = Json.toJson(testNonRepudiationIdentityData)

  implicit class RetrievalCombiner[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  val testAuthRetrievals: NonRepudiationIdentityRetrievals =
    Some(testAffinityGroup) ~
      Some(testInternalid) ~
      Some(testExternalId) ~
      Some(testAgentCode) ~
      Some(testCredentials) ~
      testConfidenceLevel ~
      Some(testNino) ~
      Some(testSautr) ~
      Some(testAuthName) ~
      Some(testEmail) ~
      testAgentInformation ~
      Some(testGroupIdentifier) ~
      Some(testCredentialRole) ~
      Some(testMdtpInformation) ~
      Some(testItmpName) ~
      Some(testItmpAddress) ~
      Some(testCredentialStrength)

}
