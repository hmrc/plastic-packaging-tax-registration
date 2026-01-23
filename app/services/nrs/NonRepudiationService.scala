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

package services.nrs

import connectors.NonRepudiationConnector
import models.nrs.{IdentityData, NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import play.api.Logger
import services.nrs.NonRepudiationService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpException, InternalServerException}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class NonRepudiationService @Inject() (
  nonRepudiationConnector: NonRepudiationConnector,
  authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  def submitNonRepudiation(
    payloadString: String,
    submissionTimestamp: ZonedDateTime,
    pptReference: String,
    userHeaders: Map[String, String]
  )(implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionAccepted] =
    for {
      identityData <- retrieveIdentityData()
      payloadChecksum = retrievePayloadChecksum(payloadString)
      userAuthToken   = retrieveUserAuthToken(hc)
      nonRepudiationMetadata = NonRepudiationMetadata("ppt",
                                                      "ppt-subscription",
                                                      "application/json",
                                                      payloadChecksum,
                                                      submissionTimestamp,
                                                      identityData,
                                                      userAuthToken,
                                                      userHeaders,
                                                      Map("pptReference" -> pptReference)
      )
      encodedPayloadString = encodePayload(payloadString)
      nonRepudiationSubmissionResponse <- retrieveNonRepudiationResponse(nonRepudiationMetadata,
                                                                         encodedPayloadString
      )
    } yield nonRepudiationSubmissionResponse

  private def encodePayload(payloadString: String): String =
    Base64.getEncoder.encodeToString(payloadString.getBytes(StandardCharsets.UTF_8))

  private def retrieveNonRepudiationResponse(
    nonRepudiationMetadata: NonRepudiationMetadata,
    encodedPayloadString: String
  )(implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionAccepted] =
    nonRepudiationConnector.submitNonRepudiation(encodedPayloadString, nonRepudiationMetadata).map {
      case response @ NonRepudiationSubmissionAccepted(_) =>
        logger.info(s"Successfully called NRS and got submissionId ${response.submissionId}")
        response
    }.recoverWith {
      case exception: HttpException =>
        logger.warn(
          s"Failed to call NRS with exception ${exception.responseCode} and ${exception.message}"
        )
        Future.failed(exception)
    }

  private def retrieveUserAuthToken(hc: HeaderCarrier): String =
    hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _                              => throw new InternalServerException("No auth token available for NRS")
    }

  private def retrievePayloadChecksum(payloadString: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(payloadString.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString

  private def retrieveIdentityData()(implicit headerCarrier: HeaderCarrier): Future[IdentityData] =
    authConnector.authorise(EmptyPredicate, nonRepudiationIdentityRetrievals).map {
      case affinityGroup ~ internalId ~
          externalId ~ agentCode ~
          credentials ~ confidenceLevel ~
          nino ~ saUtr ~
          name ~
          email ~ agentInfo ~
          groupId ~ credentialRole ~
          mdtpInfo ~ itmpName ~
          itmpAddress ~
          credentialStrength =>
        IdentityData(internalId = internalId,
                     externalId = externalId,
                     agentCode = agentCode,
                     optionalCredentials = credentials,
                     confidenceLevel = confidenceLevel,
                     nino = nino,
                     saUtr = saUtr,
                     optionalName = name,
                     email = email,
                     agentInformation = agentInfo,
                     groupIdentifier = groupId,
                     credentialRole = credentialRole,
                     mdtpInformation = mdtpInfo,
                     optionalItmpName = itmpName,
                     optionalItmpAddress = itmpAddress,
                     affinityGroup = affinityGroup,
                     credentialStrength = credentialStrength
        )
    }

}

object NonRepudiationService {

  type NonRepudiationIdentityRetrievals =
    (Option[AffinityGroup] ~ Option[String]
      ~ Option[String] ~ Option[String]
      ~ Option[Credentials] ~ ConfidenceLevel
      ~ Option[String] ~ Option[String]
      ~ Option[Name]
      ~ Option[String] ~ AgentInformation
      ~ Option[String] ~ Option[CredentialRole]
      ~ Option[MdtpInformation] ~ Option[ItmpName]
      ~ Option[ItmpAddress]
      ~ Option[String])

  val nonRepudiationIdentityRetrievals: Retrieval[NonRepudiationIdentityRetrievals] =
    Retrievals.affinityGroup and Retrievals.internalId and
      Retrievals.externalId and Retrievals.agentCode and
      Retrievals.credentials and Retrievals.confidenceLevel and
      Retrievals.nino and Retrievals.saUtr and
      Retrievals.name and
      Retrievals.email and Retrievals.agentInformation and
      Retrievals.groupIdentifier and Retrievals.credentialRole and
      Retrievals.mdtpInformation and Retrievals.itmpName and
      Retrievals.itmpAddress and
      Retrievals.credentialStrength

}
