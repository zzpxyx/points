package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import scala.collection.mutable

class PointsServiceSpec extends AnyWordSpec with Matchers {
  "addTransaction" should {
    "add a transaction" in {
      val timestamp = Instant.now()
      val transaction = Transaction("payer", 100, timestamp)
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      val service = new PointsService(priorityQueue)
      service.addTransaction(transaction)
      priorityQueue.toSeq shouldBe Seq(transaction)
    }
    "allow adding a transaction that makes a specific payer's balance negative" in {
      val payer = "payer"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer, 100, timestamp)
      val transaction2 = Transaction(payer, -200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      val service = new PointsService(priorityQueue)
      service.addTransaction(transaction1)
      service.addTransaction(transaction2)
      priorityQueue.toSeq shouldBe Seq(transaction1, transaction2)
    }
  }

  "usePoints" should {
    "use the oldest points first" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer2, 200, timestamp)
      val transaction2 = Transaction(payer1, 100, timestamp.minusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      val result = service.usePoints(150)
      result shouldBe Seq(PayerPoints(payer1, 100), PayerPoints(payer2, 50))
      priorityQueue.head shouldBe Transaction(payer2, 150, timestamp)
    }
    "use up all the points if necessary" in {
      val payer = "payer"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer, 100, timestamp)
      val transaction2 = Transaction(payer, 200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      val result = service.usePoints(300)
      result shouldBe Seq(PayerPoints(payer, 300))
      priorityQueue.isEmpty shouldBe true
    }
    "work even if some transactions have zero points" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val payer3 = "payer3"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 0, timestamp)
      val transaction2 = Transaction(payer2, 100, timestamp.plusSeconds(1))
      val transaction3 = Transaction(payer3, 0, timestamp.plusSeconds(2))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2, transaction3)
      val service = new PointsService(priorityQueue)
      val result = service.usePoints(100)
      result shouldBe Seq(PayerPoints(payer1, 0), PayerPoints(payer2, 100))
      priorityQueue.head shouldBe transaction3
    }
    "use arbitrary one if two transactions have the same timestamp" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val priorityQueue1 = mutable.PriorityQueue[Transaction]()
      val priorityQueue2 = mutable.PriorityQueue[Transaction]()
      priorityQueue1.enqueue(Transaction(payer1, 100, timestamp), Transaction(payer2, 200, timestamp))
      priorityQueue2.enqueue(Transaction(payer2, 200, timestamp), Transaction(payer1, 100, timestamp))
      val service1 = new PointsService(priorityQueue1)
      val service2 = new PointsService(priorityQueue2)
      val result1 = service1.usePoints(150)
      val result2 = service2.usePoints(150)
      val possibleResults = Set(Seq(PayerPoints(payer1, 100), PayerPoints(payer2, 50)), Seq(PayerPoints(payer2, 150)))
      Set(result1, result2) shouldBe possibleResults
    }
    "throw InsufficientPointException if there is not enough points" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 100, timestamp)
      val transaction2 = Transaction(payer2, 200, timestamp.plusSeconds(1))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2)
      val service = new PointsService(priorityQueue)
      an[InsufficientPointsException] should be thrownBy service.usePoints(500)
      priorityQueue.head shouldBe transaction1
      priorityQueue.last shouldBe transaction2
    }
  }

  "getPointsPerPayer" should {
    "get the points for each payer" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 100, timestamp)
      val transaction2 = Transaction(payer2, 200, timestamp.plusSeconds(1))
      val transaction3 = Transaction(payer1, 300, timestamp.plusSeconds(2))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2, transaction3)
      val service = new PointsService(priorityQueue)
      service.getPointsPerPayer shouldBe Map(payer1 -> 400, payer2 -> 200)
      priorityQueue.size shouldBe 3
    }
    "get the summary even if some payers have zero or negative points" in {
      val payer1 = "payer1"
      val payer2 = "payer2"
      val timestamp = Instant.now()
      val transaction1 = Transaction(payer1, 100, timestamp)
      val transaction2 = Transaction(payer2, 0, timestamp.plusSeconds(1))
      val transaction3 = Transaction(payer1, -200, timestamp.plusSeconds(2))
      val priorityQueue = mutable.PriorityQueue[Transaction]()
      priorityQueue.enqueue(transaction1, transaction2, transaction3)
      val service = new PointsService(priorityQueue)
      service.getPointsPerPayer shouldBe Map(payer1 -> -100, payer2 -> 0)
      priorityQueue.size shouldBe 3
    }
  }
}
