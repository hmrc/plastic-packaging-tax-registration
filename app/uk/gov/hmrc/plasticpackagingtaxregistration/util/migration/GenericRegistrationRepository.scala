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
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{Document, MongoClient}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig

import scala.concurrent.ExecutionContext

@Singleton
class GenericRegistrationRepository @Inject() (appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  private val client     = MongoClient(appConfig.mongoUri)
  private val db         = client.getDatabase(appConfig.mongoDb)
  private val collection = db.getCollection(appConfig.mongoCollection)

  def upgradeRegistrations(upgrader: Document => Document) =
    collection.find()
      .toFuture()
      .map { registrations =>
        registrations.foreach { reg =>
          val updatedReg = upgrader(reg)
          collection.replaceOne(filter(updatedReg.get[BsonString]("id").get), updatedReg).toFuture()
        }
      }

  private def filter(id: BsonString) =
    equal("id", id)

}
