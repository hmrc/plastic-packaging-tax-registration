/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors

import play.api.http.Status.ACCEPTED
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpException,
  HttpReadsHttpResponse,
  HttpResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.{
  NonRepudiationMetadata,
  NonRepudiationSubmissionAccepted
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationConnector @Inject() (httpClient: HttpClient, config: AppConfig)(implicit
  ec: ExecutionContext
) extends HttpReadsHttpResponse {

  def submitNonRepudiation(
    encodedPayloadString: String,
    nonRepudiationMetadata: NonRepudiationMetadata
  )(implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionAccepted] = {
    val jsonBody = Json.obj("payload" -> encodedPayloadString, "metadata" -> nonRepudiationMetadata)

    httpClient.POST[JsObject, HttpResponse](url = config.nonRepudiationSubmissionUrl,
                                            body = jsonBody,
                                            headers =
                                              Seq("X-API-Key" -> config.nonRepudiationApiKey)
    ).map {
      response =>
        response.status match {
          case ACCEPTED =>
            val submissionId = (response.json \ "nrSubmissionId").as[String]
            NonRepudiationSubmissionAccepted(submissionId)
          case _ =>
            throw new HttpException(response.body, response.status)
        }
    }
  }

}
