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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors

import base.Injector
import base.data.SubscriptionTestData
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import connectors.HipSubscriptionsConnector
import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import models.eis.EISError
import models.eis.subscription.Subscription

import java.util.UUID

class HipSubscriptionConnectorSpec
    extends ConnectorISpec with Injector with ScalaFutures with SubscriptionTestData
    with EitherValues {

  override def overrideConfig: Map[String, Any] =
    Map(
      "microservice.services.hip.host"                   -> wireHost,
      "microservice.services.hip.port"                   -> wirePort,
      "microservice.services.nrs.host"                   -> wireHost,
      "microservice.services.nrs.port"                   -> wirePort,
      "microservice.services.tax-enrolments.host"        -> wireHost,
      "microservice.services.tax-enrolments.port"        -> wirePort,
      "microservice.services.enrolment-store-proxy.port" -> wirePort
    )

  private lazy val connector =
    app.injector.instanceOf[HipSubscriptionsConnector]

  private val pptSubscriptionDisplayTimer    = "ppt.subscription.display.timer"

  "Subscription connector" when {

    "requesting a subscription" should {
      "handle a 200" in {

        val pptReference = UUID.randomUUID().toString
        stubSubscriptionDisplay(pptReference, ukLimitedCompanySubscription)

        val res: Either[Int, Subscription] = await(connector.getSubscription(pptReference))

        res.toOption mustBe Some(ukLimitedCompanySubscription)

        getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 500)) { statusCode =>
        s"return $statusCode" when {
          s"$statusCode is returned from downstream service" in {
            val pptReference = UUID.randomUUID().toString
            val errors =
              createErrorResponse(code = "INVALID_VALUE",
                                  reason =
                                    "Some errors occurred"
              )

            stubSubscriptionDisplayFailure(httpStatus = statusCode,
                                           errors = errors,
                                           pptReference = pptReference
            )

            val res = await(connector.getSubscription(pptReference))

            res.left.value mustBe statusCode
            getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
          }
        }
      }
    }
  }

  private def createErrorResponse(code: String, reason: String): Seq[EISError] =
    Seq(EISError(code, reason))

  private def stubSubscriptionDisplay(pptReference: String, response: Subscription): Unit =
    stubFor(
      get(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(
              Json.stringify(
                Json.obj(
                  "success" -> Json.toJson(response)
                )
              )
            )
        )
    )


  private def stubSubscriptionDisplayFailure(
    pptReference: String,
    httpStatus: Int,
    errors: Seq[EISError]
  ): Any =
    stubFor(
      get(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

}
