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

package uk.gov.hmrc.plasticpackagingtaxregistration.util.migration

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import play.api.Logger

@Singleton
class RegistrationUpgrader @Inject() (
  genericRegistrationRepository: GenericRegistrationRepository
) {

  private val ORG_DETAILS    = "organisationDetails"
  private val INCORP_DETAILS = "incorporationDetails"
  private val REGISTRATION   = "registration"
  private val BV_STATUS_OLD  = "businessVerificationStatus"
  private val BV_STATUS_NEW  = "verificationStatus"
  private val IDS_MATCH      = "identifiersMatch"

  private val logger = Logger(this.getClass)

  // Do it on startup, when the singleton is created. This is done eagerly in 'production' mode.
  upgradeRegistrations()

  def upgradeRegistrations() = {
    logger.info("Upgrading any existing in-flight registrations")
    genericRegistrationRepository.upgradeRegistrations(upgradeRegistration)
  }

  private def upgradeRegistration(reg: Document): Document = {
    val organisationDetails: Option[BsonDocument] = reg.get[BsonDocument](ORG_DETAILS)
    if (organisationDetails.exists(od => od.containsKey(INCORP_DETAILS))) {
      val incorporationDetails = organisationDetails.get.getDocument(INCORP_DETAILS)
      if (incorporationDetails.containsKey(REGISTRATION)) {
        val bvStatus: Option[BsonString] =
          if (incorporationDetails.containsKey(BV_STATUS_OLD))
            Some(incorporationDetails.getString(BV_STATUS_OLD))
          else None
        val registration = incorporationDetails.getDocument(REGISTRATION)

        if (bvStatus.isDefined) {
          logger.info(s"Upgrading in-flight registration with id [${reg.getString("id")}]")
          incorporationDetails.remove(BV_STATUS_OLD)
          registration.append(BV_STATUS_NEW, bvStatus.get)
          registration.append(IDS_MATCH, BsonString("true")) // Do it matter what we set this to?
        } else
          logger.info(
            s"Not upgrading compatible in-flight registration with id [${reg.getString("id")}]"
          )
      } else
        logger.info(
          s"Not upgrading compatible in-flight registration with id [${reg.getString("id")}]"
        )
    } else
      logger.info(
        s"Not upgrading compatible in-flight registration with id [${reg.getString("id")}]"
      )

    reg
  }

}
