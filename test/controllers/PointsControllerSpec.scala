package controllers

import akka.actor.ActorSystem
import models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.NoContent
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class PointsControllerSpec extends AnyWordSpec with Matchers with ScalaFutures {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher

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
      contentAsJson(result).as[Seq[PayerPoints]] shouldBe Seq(PayerPoints(payer1, -100), PayerPoints(payer2, -50))
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

  "Endpoints" should {
    "work together when there are enough points" in {
      val transactionBodySeq = Seq(
        """{ "payer": "payer1", "points": 1000, "timestamp": "2020-11-02T14:00:00Z" }""",
        """{ "payer": "payer2", "points": 200, "timestamp": "2020-10-31T11:00:00Z" }""",
        """{ "payer": "payer1", "points": -200, "timestamp": "2020-10-31T15:00:00Z" }""",
        """{ "payer": "payer3", "points": 10000, "timestamp": "2020-11-01T14:00:00Z" }""",
        """{ "payer": "payer1", "points": 300, "timestamp": "2020-10-31T10:00:00Z" }""")
      val service = new PointsService()
      val controller = new PointsController(stubControllerComponents(), service)
      val usePointResult = transactionBodySeq.foldLeft(Future.successful(NoContent)) { (future, body) =>
        future.flatMap { _ =>
          val request = FakeRequest(POST, "/points/v1/transactions").withJsonBody(Json.parse(body))
          call(controller.addTransaction(), request)
        }
      }.flatMap { _ =>
        val usePointsRequest = UsePointsRequest(5000)
        val request = FakeRequest(POST, "/points/v1/balance").withJsonBody(Json.toJson(usePointsRequest))
        call(controller.usePoints(), request)
      }
      contentAsJson(usePointResult).as[Seq[PayerPoints]] shouldBe
        Seq(PayerPoints("payer1", -100), PayerPoints("payer2", -200), PayerPoints("payer3", -4700))
      val balance = {
        val request = FakeRequest(GET, "/points/v1/balance")
        call(controller.getPointsPerPayer, request)
      }
      contentAsJson(balance).as[Map[String, Int]] shouldBe Map("payer1" -> 1000, "payer2" -> 0, "payer3" -> 5300)
    }
  }
}
