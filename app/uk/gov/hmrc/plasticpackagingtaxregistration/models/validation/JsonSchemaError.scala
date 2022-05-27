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

package uk.gov.hmrc.plasticpackagingtaxregistration.models.validation

import play.api.libs.json.{__, JsError, JsObject, JsString, JsSuccess, JsValue, Json, Reads, Writes}

import scala.util.{Failure, Success, Try}
sealed trait SchemaError

object SchemaError {

  case class NestedSchemaError(
    schemaPath: String,
    keyword: String,
    instancePath: String,
    errors: Map[String, Seq[SchemaError]]
  ) extends SchemaError

  case class SimpleSchemaError(message: String) extends SchemaError

  private def fromJson(json: JsValue): SchemaError =
    json match {
      case msg: JsString => SimpleSchemaError(msg.as[String])
      case json: JsObject =>
        NestedSchemaError(json("schemaPath").as[String],
                          json("keyword").as[String],
                          json("instancePath").as[String],
                          json("errors").as[Map[String, Seq[SchemaError]]]
        )
      case _ => SimpleSchemaError("Unknown Error")
    }

  private def toJson(se: SchemaError): JsValue =
    se match {
      case SimpleSchemaError(msg) => JsString(msg)
      case NestedSchemaError(sp, k, ip, e) =>
        Json.obj("schemaPath"   -> sp,
                 "keyword"      -> k,
                 "instancePath" -> ip,
                 "errors"       -> Json.toJson(e)
        )
    }

  implicit val reads: Reads[SchemaError] = Reads { json =>
    Try {
      fromJson(json)
    } match {
      case Failure(exception) => JsError(__, exception.getMessage)
      case Success(value)     => JsSuccess(value)
    }
  }

  implicit val writes: Writes[SchemaError] = Writes(se => toJson(se))

}
