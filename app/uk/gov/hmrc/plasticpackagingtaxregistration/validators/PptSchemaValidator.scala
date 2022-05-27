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

import com.eclipsesource.schema.drafts.Version7._
import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtaxregistration.models.validation.SchemaError

import scala.annotation.tailrec

class PptSchemaValidator(schemaFile: String) {

  private val logger = Logger(this.getClass)

  lazy val schema: SchemaType = {
    val stream: InputStream = getClass.getResourceAsStream(schemaFile)
    val linesString: String = scala.io.Source.fromInputStream(stream).mkString
    val json                = Json.parse(linesString.trim).as[JsObject]
    json.as[SchemaType]
  }

  @tailrec
  private def buildSchemaError(x: Any, acc: Seq[SchemaError]): Seq[SchemaError] =
    x match {
      case jsons: Seq[Any] =>
        val fromHead = jsons.headOption match {
          case Some(js: JsValue) => js.validate[SchemaError].asOpt.toSeq
          case _                 => Nil
        }

        buildSchemaError(jsons(0), acc ++ fromHead)

      case json: JsValue =>
        acc ++ json.validate[SchemaError].asOpt.toSeq

      case _ => Seq.empty
    }

  def validate[T](data: T)(implicit writes: Writes[T]): JsResult[JsValue] = {
    val requestPayload = Json.toJson(data)

    val res: JsResult[JsValue] = SchemaValidator(Some(com.eclipsesource.schema.drafts.Version7))
      .validate(schema, requestPayload)

    res.fold(
      (errors: Seq[(JsPath, Seq[JsonValidationError])]) => {
        val report: Seq[(String, SchemaError)] = for {
          (path, errorList)   <- errors
          jsonValidationError <- errorList
          schemaError         <- buildSchemaError(jsonValidationError.args, Seq.empty)
        } yield (jsonValidationError.message, schemaError)

        val errorObjects = Json.toJson(report)
        logger.warn(
          s"PptSchemaValidator:$schemaFile: - Schema validation errors: ${Json.prettyPrint(errorObjects)}"
        )
      },
      _ => logger.info(s"PptSchemaValidator:$schemaFile: - Schema validation success")
    )

    res
  }

}

object PptSchemaValidator {

  lazy val subscriptionValidator = new PptSchemaValidator(
    "/api-docs/api-1711-ppt-subscription-create-1.6.0.json"
  )

}
