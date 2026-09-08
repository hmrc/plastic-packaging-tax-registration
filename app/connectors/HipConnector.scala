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

package connectors

import config.AppConfig
import models.eis.EISError
import models.eis.subscription.create.{EISSubscriptionFailureResponse, SubscriptionFailureResponseWithStatusCode}
import models.hip.HipPlatformErrors.*
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

trait HipConnector extends Logging {

  val appConfig: AppConfig

  def headers: Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT      -> MimeTypes.JSON,
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "PPT",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${appConfig.hipAuthorizationToken}"
    )

  lazy val correlationid = headers.toMap.getOrElse("correlationid", "NOT FOUND")

  private def mkErr(code: String, text: String, status: Int = 422) = {
    SubscriptionFailureResponseWithStatusCode(
      EISSubscriptionFailureResponse(
        Seq(EISError(code, text))
      ), status)
  }

  val subscriptionUpdate422ResponseMappings: Map[String, SubscriptionFailureResponseWithStatusCode] = Map(
    "001" -> mkErr("INVALID_REGIME","The remote endpoint has indicated that the REGIME provided is invalid."),
    "004" -> mkErr("DUPLICATE_SUBMISSION", "The remote endpoint has indicated that duplicate submission acknowledgment reference.", 409),
    "087" -> mkErr("TBC", "not yet known"), // TODO need confirmation from Hip team
    "089" -> mkErr("INVALID_PPT_REFERENCE_NUMBER", "The remote endpoint has indicated that the PPT Reference Number provided is invalid."),
    "090" -> mkErr("CANNOT_CREATE_PARTNERSHIP_SUBSCRIPTION", "The remote end point has indicated cannot Create Partnership Subscription."),
    "999" -> mkErr("SERVER_ERROR", "IF is currently experiencing problems that require live service intervention.", 500)
  )

  // use only for 400 500 and 503
  def parseHipErrorEnvelopeResponse(response: HttpResponse): HipErrorTrait = {
    if (!List(400, 500, 503).contains(response.status)) {
      throw new IllegalArgumentException("")
    } else {
      response.json.asOpt[HipErrorWrapper] match {
        case Some(HipErrorWrapper(_, sysErr: HipSystemErrorObject)) => sysErr
        case Some(HipErrorWrapper(_, hipFails: HipFailuresErrorArray)) => hipFails
        case _ => HipUnexpectedError(response.status, response.body)
      }
    }
  }

}
