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
import org.mongodb.scala.bson.{BsonBoolean, BsonDocument, BsonString}
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

  private val BUSINESS_REGISTERED_ADDRESS = "businessRegisteredAddress"
  private val COUNTRY_CODE                = "countryCode"

  private val PRIMARY_CONTACT_DETAILS = "primaryContactDetails"
  private val ADDRESS                 = "address"

  private val logger = Logger(this.getClass)

  // Do it on startup, when the singleton is created. This is done eagerly in 'production' mode.
  upgradeRegistrations()

  def upgradeRegistrations() = {
    logger.info("Upgrading any existing in-flight registrations")
    //genericRegistrationRepository.upgradeRegistrations(upgradeIncorporationDetails)
    genericRegistrationRepository.upgradeRegistrations(addMissingCountryCodes)
  }

  private def addMissingCountryCodes(reg: Document): Document = {
    val organisationDetails: Option[BsonDocument] = reg.get[BsonDocument](ORG_DETAILS)
    if (organisationDetails.exists(od => od.containsKey(BUSINESS_REGISTERED_ADDRESS))) {
      val businessRegisteredAddress =
        organisationDetails.get.getDocument(BUSINESS_REGISTERED_ADDRESS)
      if (businessRegisteredAddress.containsKey(COUNTRY_CODE))
        logger.info(
          s"Not adding business registered address country code to in-flight registration with id [${reg.getString("id")}]"
        )
      else {
        logger.info(
          s"Adding business registered address country code to in-flight registration with id [${reg.getString("id")}]"
        )
        businessRegisteredAddress.append(COUNTRY_CODE, BsonString("GB"))
      }
    } else
      logger.info(
        s"Not adding business registered address country code to in-flight registration with id [${reg.getString("id")}]"
      )

    val primaryContactDetails: Option[BsonDocument] = reg.get[BsonDocument](PRIMARY_CONTACT_DETAILS)
    if (primaryContactDetails.exists(pcd => pcd.containsKey(ADDRESS))) {
      val address = primaryContactDetails.get.getDocument(ADDRESS)
      if (address.containsKey(COUNTRY_CODE))
        logger.info(
          s"Not adding primary contact address country code to in-flight registration with id [${reg.getString("id")}]"
        )
      else {
        logger.info(
          s"Adding primary contact address country code to in-flight registration with id [${reg.getString("id")}]"
        )
        address.append(COUNTRY_CODE, BsonString("GB"))
      }
    } else
      logger.info(
        s"Not adding primary contact address country code to in-flight registration with id [${reg.getString("id")}]"
      )

    reg
  }

  private def upgradeIncorporationDetails(reg: Document): Document = {
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
          registration.append(IDS_MATCH, BsonBoolean(true)) // Does it matter what we set this to?
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
