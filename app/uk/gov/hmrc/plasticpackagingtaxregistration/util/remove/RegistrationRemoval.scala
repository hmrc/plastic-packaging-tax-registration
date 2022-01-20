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

package uk.gov.hmrc.plasticpackagingtaxregistration.util.remove

import com.google.inject.{Inject, Singleton}
import com.mongodb.BasicDBObject
import org.mongodb.scala.result.DeleteResult
import play.api.Logger
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepositoryImpl

@Singleton
class RegistrationRemoval @Inject() (registrationRepositoryImpl: RegistrationRepositoryImpl) {

  private val logger = Logger(this.getClass)

  // Do it on startup, when the singleton is created. This is done eagerly in 'production' mode.
  removeRegistrations()

  def removeRegistrations(): Unit = {
    logger.info("Removing in-flight registrations")

    registrationRepositoryImpl.collection.deleteMany(new BasicDBObject()).subscribe(
      (dr: DeleteResult) => logger.info(s"Removed ${dr.getDeletedCount} in-flight registrations"),
      (e: Throwable) => println(s"Error when removing in-flight registrations $e")
    )
  }

}
