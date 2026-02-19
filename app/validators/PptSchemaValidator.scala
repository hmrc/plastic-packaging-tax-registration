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

package validators

import cats.data.NonEmptyList
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchemaFactory, SpecVersion, ValidationMessage}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import models.validation.JsonSchemaError

import scala.jdk.CollectionConverters._

class PptSchemaValidator(schemaFile: String) {

  private val logger = LoggerFactory.getLogger(getClass.getCanonicalName)
  private val objectMapper = new ObjectMapper()

  private lazy val schema = {
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    val stream = getClass.getResourceAsStream(schemaFile)
    Option(stream)
      .map(factory.getSchema)
      .getOrElse(throw new IllegalStateException(s"Could not find resource '$schemaFile'"))
  }

  def validate[T](data: T)(implicit writes: Writes[T]): Either[NonEmptyList[JsonSchemaError], Unit] = {
  val dataJson = Json.toJson(data)
  val jacksonNode = objectMapper.readTree(dataJson.toString())
  val errors = schema.validate(jacksonNode).asScala.toList

  if (errors.isEmpty) {
    Right(())
  } else {
    val schemaErrors = errors.map { (vm: ValidationMessage) =>
      JsonSchemaError(
        Some(schemaFile),
        keyword = vm.getType,
        instancePath = vm.getMessage
      )
    }
    NonEmptyList.fromList(schemaErrors) match {
      case Some(nonEmptyErrors) =>
        logger.warn(Json.prettyPrint(Json.toJson(nonEmptyErrors.toList)))
        Left(nonEmptyErrors)
      case None => Right(())
    }
  }
}
}



object PptSchemaValidator {

  lazy val subscriptionValidator = new PptSchemaValidator(
    "/api-docs/api-1711-ppt-subscription-create-1.6.0.json"
  )

}
