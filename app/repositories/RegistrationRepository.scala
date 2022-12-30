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

package repositories

import java.util.concurrent.TimeUnit

import com.codahale.metrics.Timer
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import com.mongodb.client.model.Indexes.ascending
import javax.inject.Inject
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logger
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import config.AppConfig
import models.Registration

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RegistrationRepositoryImpl])
trait RegistrationRepository {
  def findByRegistrationId(id: String): Future[Option[Registration]]
  def create(registration: Registration): Future[Registration]
  def update(registration: Registration): Future[Option[Registration]]
  def delete(pptId: String): Future[Unit]
}

class RegistrationRepositoryImpl @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Registration](collectionName = "registrations",
                                              mongoComponent = mongoComponent,
                                              domainFormat = MongoSerialisers.format,
                                              indexes = Seq(
                                                IndexModel(
                                                  ascending("lastModifiedDateTime"),
                                                  IndexOptions().name("ttlIndex").expireAfter(
                                                    appConfig.dbTimeToLiveInSeconds,
                                                    TimeUnit.SECONDS
                                                  )
                                                ),
                                                IndexModel(ascending("id"),
                                                           IndexOptions().name("idIdx").unique(true)
                                                )
                                              ),
                                              replaceIndexes = true
    ) with RegistrationRepository {

  private val logger = Logger(this.getClass)

  private def filter(id: String) =
    equal("id", Codecs.toBson(id))

  private def newMongoDBTimer(name: String): Timer = metrics.defaultRegistry.timer(name)

  override def findByRegistrationId(id: String): Future[Option[Registration]] = {
    val findStopwatch = newMongoDBTimer("ppt.registration.mongo.find").time()
    collection.find(filter(id)).headOption().andThen {
      case _ => findStopwatch.stop()
    }
  }

  override def create(registration: Registration): Future[Registration] = {
    val createStopwatch     = newMongoDBTimer("ppt.registration.mongo.create").time()
    val updatedRegistration = registration.updateLastModified()
    collection.insertOne(updatedRegistration).toFuture().andThen {
      case _ => createStopwatch.stop()
    }.map(_ => updatedRegistration)
  }

  override def update(registration: Registration): Future[Option[Registration]] = {
    val updateStopwatch     = newMongoDBTimer("ppt.registration.mongo.update").time()
    val updatedRegistration = registration.updateLastModified()
    collection.replaceOne(filter(registration.id), updatedRegistration).toFuture().map(
      updateResult => if (updateResult.getModifiedCount == 1) Some(updatedRegistration) else None
    ).andThen {
      case _ => updateStopwatch.stop()
    }
  }

  override def delete(pptId: String): Future[Unit] = {
    val deleteStopwatch = newMongoDBTimer("ppt.registration.mongo.delete").time()
    collection.deleteOne(filter(pptId)).toFuture().andThen {
      case _ => deleteStopwatch.stop()
    }.map { result =>
      if (result.getDeletedCount != 1) logger.error(s"Failed to delete registration id: $pptId")
    }
  }

}

object MongoSerialisers {

  implicit val mongoDateTimeFormat: Format[DateTime] =
    uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats.dateTimeFormat

  implicit val format: Format[Registration] = Json.format[Registration]
}
