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

package uk.gov.hmrc.plasticpackagingtaxregistration.repositories

import org.joda.time.DateTime
import reactivemongo.api.commands.Command
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{BSONSerializationPack, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONLong}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

trait Timestamped {
  val lastModifiedDateTime: Option[DateTime]
}

trait TTLIndexing[A <: Timestamped, ID] {
  self: ReactiveRepository[A, ID] =>

  val expireAfterSeconds: Long

  lazy val ttlIndex: Index = Index(key = Seq(LastModifiedDateField -> IndexType.Ascending),
                                   name = Some(TtlIndex),
                                   options = BSONDocument(ExpireAfterSeconds -> expireAfterSeconds)
  )

  private val LastModifiedDateField = "lastModifiedDateTime"
  private val TtlIndex              = "ttlIndex"
  private val ExpireAfterSeconds    = "expireAfterSeconds"

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    logger.info(s"Creating time to live for entries in ${collection.name} to $expireAfterSeconds seconds")
    for {
      currentIndexes <- collection.indexesManager.list()
      _              <- updateTtlIndex(currentIndexes)
      result <- Future.sequence((ttlIndex +: indexes).map {
        collection.indexesManager.ensure
      })
    } yield result
  }

  def updateTtlIndex(indexes: List[Index])(implicit ec: ExecutionContext): Future[BSONDocument] =
    indexes.find(
      index => index.eventualName == TtlIndex && getExpireAfterSecondsOptionOf(index) != expireAfterSeconds
    ) match {
      case Some(_) => updateExpiryDate
      case None    => Future.successful(BSONDocument())
    }

  def updateExpiryDate(implicit ec: ExecutionContext): Future[BSONDocument] = {
    val runner = Command.run(BSONSerializationPack, failover = RepositorySettings.failoverStrategy)
    val command = BSONDocument("collMod" -> collectionName,
                               "index" -> BSONDocument("keyPattern" -> BSONDocument(LastModifiedDateField -> 1),
                                                       ExpireAfterSeconds -> expireAfterSeconds
                               )
    )
    logger.info(s"Updating existing index $TtlIndex, command: ${BSONDocument.pretty(command)}")
    runner.apply(collection.db, runner.rawCommand(command)).one(ReadPreference.primaryPreferred)
  }

  def getExpireAfterSecondsOptionOf(idx: Index): Long =
    idx.options.getAs[BSONLong](ExpireAfterSeconds).getOrElse(BSONLong(0)).as[Long]

}
