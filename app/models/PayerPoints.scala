package models

import play.api.libs.json.{Json, OFormat}

case class PayerPoints(payer: String, points: Int)

object PayerPoints {
  implicit val format: OFormat[PayerPoints] = Json.format[PayerPoints]
}
