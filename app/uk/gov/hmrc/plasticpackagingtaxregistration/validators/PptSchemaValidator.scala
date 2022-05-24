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

import com.eclipsesource.schema.drafts.Version7._
import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import play.api.Logger

import java.io.InputStream
import play.api.libs.json.{JsResult, JsValue, Json}

class PptSchemaValidator {

  private val logger = Logger(this.getClass)

  def validate(schemaFile: String, requestPayload: JsValue): JsResult[JsValue] = {

    val stream: InputStream = getClass.getResourceAsStream(schemaFile)
    val lines: Iterator[String] = scala.io.Source.fromInputStream(stream).getLines
    val linesString: String = lines.foldLeft[String]("")((x, y) => x.trim ++ y.trim)

    val res = SchemaValidator(Some(com.eclipsesource.schema.drafts.Version7))
      .validate(Json.fromJson[SchemaType](Json.parse(linesString.trim)).get, requestPayload)

    res.fold(
      errors => {

        val asJson: JsValue = errors.flatMap(x => x._2).head.args.head.asInstanceOf[JsValue]
        val validationErrors = (asJson \ "errors").get

        logger.warn(
          s"PptSchemaValidator:$schemaFile: - Schema validation errors: $validationErrors"
        )

      },
      _ => logger.info(s"PptSchemaValidator:$schemaFile: - Schema validation success"))

    res

  }

}
