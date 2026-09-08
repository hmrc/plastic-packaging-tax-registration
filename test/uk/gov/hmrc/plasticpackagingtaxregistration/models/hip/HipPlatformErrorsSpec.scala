package uk.gov.hmrc.plasticpackagingtaxregistration.models.hip

import models.hip.HipPlatformErrors.HipErrorWrapper
import org.scalatest.Inspectors.forAll
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class HipPlatformErrorsSpec extends AnyWordSpec {

  val failuresArrayErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "Type of Failure",
      |        "reason": "Reason for Failure"
      |      },
      |      {
      |        "type": "Another type of Failure",
      |        "reason": "More reasons"
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin)

  val objErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HoD",
      |  "response": {
      |    "error": {
      |      "code": "400",
      |      "logID": "00000000000000000000000000000000",
      |      "message": "String"
      |    }
      |  }
      |}
      |""".stripMargin)


  "HipPlatformErrors" should {
    "be read and written correctly" in {
      forAll(Seq(failuresArrayErr, objErr)) { jsValue =>
        assert (Json.toJson(jsValue.as[HipErrorWrapper]) === jsValue)
      }
    }
  }

}
