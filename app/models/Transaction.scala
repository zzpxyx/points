package models

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class Transaction(payer: String, points: Int, timestamp: Instant) extends Ordered[Transaction] {
  override def compare(that: Transaction): Int = {
    that.timestamp.compareTo(this.timestamp) // Used by priority queue, so early one should have a higher priority.
  }
}

object Transaction {
  implicit val format: OFormat[Transaction] = Json.format[Transaction]
}
