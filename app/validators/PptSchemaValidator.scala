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

import java.io.InputStream

import cats.data.NonEmptyList
import org.slf4j.LoggerFactory
import play.api.libs.json.*
import models.validation.JsonSchemaError
import com.eclipsesource.schema._
import com.eclipsesource.schema.drafts.Version7
import Version7._

import scala.util.Try
import org.apache.pekko.util.Version

class PptSchemaValidator(schemaFile: String) {

  private val logger = LoggerFactory.getLogger(getClass.getCanonicalName)

  private lazy val schemaFileAsString: Try[String] =
    Try {
      val stream: InputStream = getClass.getResourceAsStream(schemaFile)
      scala.io.Source.fromInputStream(stream).mkString
    }

  private lazy val parsedSchema: Try[SchemaType] =
    schemaFileAsString.flatMap { s =>
      JsonSource.schemaFromString(s) match {
        case JsSuccess(schema, _) => scala.util.Success(schema)
        case JsError(errors) => scala.util.Failure(new Exception(JsError.toJson(errors).toString()))
      }

    }

  def validate[T](
    data: T
  )(implicit writes: Writes[T]): Either[NonEmptyList[JsonSchemaError], Unit] = {
    val dataJson = Json.toJson(data).toString()
    
    parsedSchema.toEither.left
    .map( t=> NonEmptyList.one(JsonSchemaError(Some(schemaFile), "SCHEMA_LOAD_ERROR", t.getMessage)))
    .flatMap { schema =>
      val validator = SchemaValidator(Some(Version7))
      validator.validate(schema, dataJson) match {
        case JsSuccess(_, _) => Right(())
        case JsError(errors) =>
          val schemaErrors = errors.toList.flatMap { case (path, validationErrors) =>
            validationErrors.map { ve =>
              JsonSchemaError(
                Some(path.toString()),
                keyword = ve.message,
                instancePath = path.toString()
              )
            }
          }
          NonEmptyList.fromList(schemaErrors) match {
            case Some(nonEmptyErrors) => 
              logger.warn(Json.prettyPrint(Json.toJson(nonEmptyErrors.toList))) 
              Left(nonEmptyErrors)
            case None => 
              Right(())
          }
    }
  }
  }

}

object PptSchemaValidator {

  lazy val subscriptionValidator = new PptSchemaValidator(
    "/api-docs/api-1711-ppt-subscription-create-1.6.0.json"
  )

}
