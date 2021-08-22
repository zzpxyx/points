package models

import play.api.libs.json.{Json, OFormat}

case class UsePointsRequest(points: Int)

object UsePointsRequest {
  implicit val format: OFormat[UsePointsRequest] = Json.format[UsePointsRequest]
}
