package controllers

import akka.actor.ActorSystem
import models._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.Instant
import scala.collection.mutable

class PointsControllerSpec extends AnyWordSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem()

  "addTransaction" should {
    "return NO_CONTENT when adding a transaction successfully" in {
      val transaction = Transaction("payer", 100, Instant.now())
      val service = new PointsService()
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(POST, "/points/v1/transactions").withJsonBody(Json.toJson(transaction))
      val result = call(controller.addTransaction(), request)
      status(result) shouldBe NO_CONTENT
    }
    "return BAD_REQUEST if the request does not have a valid JSON body" in {
      val service = new PointsService()
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(POST, "/points/v1/transactions").withJsonBody(Json.parse("{}"))
      val result = call(controller.addTransaction(), request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe InvalidJsonException.getMessage
    }
  }

  "usePoints" should {
    "return OK with a list of payer points if there are enough points" in {
      val usePointsRequest = UsePointsRequest(150)
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 100, timestamp)
      val transaction2 = Transaction(payer2, 200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction](transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(POST, "/points/v1/balance").withJsonBody(Json.toJson(usePointsRequest))
      val result = call(controller.usePoints(), request)
      status(result) shouldBe OK
      contentAsJson(result).as[Seq[PayerPoints]] shouldBe Seq(PayerPoints(payer1, 100), PayerPoints(payer2, 50))
    }
    "return BAD_REQUEST if there are not enough points" in {
      val usePointsRequest = UsePointsRequest(400)
      val timestamp = Instant.now()
      val transaction1 = Transaction("payer1", 100, timestamp)
      val transaction2 = Transaction("payer2", 200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction](transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(POST, "/points/v1/balance").withJsonBody(Json.toJson(usePointsRequest))
      val result = call(controller.usePoints(), request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe InsufficientPointsException.getMessage
    }
    "return BAD_REQUEST if the request does not have a valid JSON body" in {
      val service = new PointsService()
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(POST, "/points/v1/balance").withJsonBody(Json.parse("{}"))
      val result = call(controller.usePoints(), request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe InvalidJsonException.getMessage
    }
  }

  "getPointsPerPayer" should {
    "return OK with a balance summary" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 100, timestamp)
      val transaction2 = Transaction(payer2, 200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction](transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      val controller = new PointsController(stubControllerComponents(), service)
      val request = FakeRequest(GET, "/points/v1/balance")
      val result = call(controller.getPointsPerPayer, request)
      status(result) shouldBe OK
      contentAsJson(result).as[Map[String, Int]] shouldBe Map(payer1 -> 100, payer2 -> 200)
    }
  }
}
