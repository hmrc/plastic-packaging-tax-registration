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

package uk.gov.hmrc.plasticpackagingtaxregistration.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  lazy val selfHost: String                = servicesConfig.baseUrl("self")
  lazy val eisHost: String                 = servicesConfig.baseUrl("eis")
  lazy val nrsHost: String                 = servicesConfig.baseUrl("nrs")
  lazy val taxEnrolmentsHost: String       = servicesConfig.baseUrl("tax-enrolments")
  lazy val enrolmentStoreProxyHost: String = servicesConfig.baseUrl("enrolment-store-proxy")

  val authBaseUrl: String      = servicesConfig.baseUrl("auth")
  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")
  val dbTimeToLiveInSeconds    = config.get[Int]("mongodb.timeToLiveInSeconds")

  val eisEnvironment = config.get[String]("eis.environment")

  def subscriptionStatusUrl(safeNumber: String): String =
    s"$eisHost/cross-regime/subscription/PPT/SAFE/${safeNumber}/status"

  def subscriptionCreateUrl(safeNumber: String): String =
    s"$eisHost/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}"

  val bearerToken: String = s"Bearer ${config.get[String]("microservice.services.eis.bearerToken")}"

  lazy val nonRepudiationSubmissionUrl: String = s"${nrsHost}/submission"

  lazy val nonRepudiationApiKey: String =
    servicesConfig.getString("microservice.services.nrs.api-key")

  def taxEnrolmentsSubscriptionsSubscriberUrl(formBundleId: String) =
    s"$taxEnrolmentsHost/tax-enrolments/subscriptions/$formBundleId/subscriber"

  def taxEnrolmentsES8AssignUserToGroupUrl(groupId: String, enrolmentKey: String) =
    s"$taxEnrolmentsHost/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"

  def taxEnrolmentsES11AssignUserToEnrolmentUrl(userId: String, enrolmentKey: String) =
    s"$taxEnrolmentsHost/tax-enrolments/users/$userId/enrolments/$enrolmentKey"

  lazy val enrolmentStoreProxyES20QueryKnownFactsUrl =
    s"$enrolmentStoreProxyHost/enrolment-store-proxy/enrolment-store/enrolments"

  def enrolmentStoreProxyES1QueryGroupsWithEnrolmentUrl(enrolmentKey: String) =
    s"$enrolmentStoreProxyHost/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"

  val nrsRetries: Seq[FiniteDuration] = config.get[Seq[FiniteDuration]]("nrs.retries")
}
