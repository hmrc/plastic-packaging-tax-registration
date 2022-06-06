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

package uk.gov.hmrc.plasticpackagingtaxregistration.validators

import java.io.InputStream

import cats.data.NonEmptyList
import io.circe.schema.ValidationError
import org.slf4j.LoggerFactory
import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtaxregistration.models.validation.JsonSchemaError

import scala.util.Try

class PptSchemaValidator(schemaFile: String) {

  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)

  private lazy val schemaFileAsString: Try[String] = {
    Try {
      val stream: InputStream = getClass.getResourceAsStream(schemaFile)
      scala.io.Source.fromInputStream(stream).mkString
    }
  }

  private lazy val circeSchema: Try[io.circe.schema.Schema] = schemaFileAsString.flatMap(io.circe.schema.Schema.loadFromString)

  def buildSchemaError(circeError: ValidationError): JsonSchemaError = {
    JsonSchemaError(
      circeError.schemaLocation,
      circeError.keyword,
      circeError.location

    )
  }

  def validate[T](data: T)(implicit writes: Writes[T]) : Either[NonEmptyList[JsonSchemaError], Unit] = {
    val dataJson = Json.toJson(data).toString()
    val result = for {
      js <- io.circe.parser.parse(dataJson).left
        .map(pf => NonEmptyList.one(ValidationError("SCHEMA_LOAD_ERROR", pf.toString, schemaFile, None)))
      schema <- circeSchema.toEither.left
        .map(t => NonEmptyList.one(ValidationError("SCHEMA_LOAD_ERROR", t.getMessage, schemaFile, None)))
      _ <- schema.validate(js).toEither
    } yield Unit

    result match {
      case Left(errs) =>
        val schemaErrors = errs.map(buildSchemaError)
        logger.warn(Json.prettyPrint(Json.toJson(schemaErrors.toList)))
        Left(schemaErrors)
      case Right(_) =>
        Right(Unit)
    }
  }
}

object PptSchemaValidator {

  lazy val subscriptionValidator = new PptSchemaValidator(
    "/api-docs/api-1711-ppt-subscription-create-1.6.0.json"
  )

}
