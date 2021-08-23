package models.service

import models.exception.InsufficientPointsException
import models.{PayerPoints, Transaction}

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * The service for bookkeeping points. Note that this service uses a mutable priority queue.
 *
 * @param priorityQueue A mutable priority queue to store transactions.
 */
class PointsService(priorityQueue: mutable.PriorityQueue[Transaction]) {
  private val pointsPerPayer: mutable.Map[String, Int] = mutable.Map().withDefaultValue(0) ++
    priorityQueue.toSeq.groupBy(_.payer).map {
      case (payer, transactionSeq) => payer -> transactionSeq.map(_.points).sum
    }

  def this() = {
    this(mutable.PriorityQueue())
  }

  /**
   * Add a transaction.
   *
   * @param transaction The transaction to be added.
   */
  def addTransaction(transaction: Transaction): Unit = {
    priorityQueue.enqueue(transaction)
    pointsPerPayer.update(transaction.payer, pointsPerPayer(transaction.payer) + transaction.points)
  }

  /**
   * Use points from the oldest transactions.
   *
   * @param points The number of points to use.
   * @return A list showing how many points are used for each payer.
   */
  @throws[InsufficientPointsException.type]
  def usePoints(points: Int): Seq[PayerPoints] = {
    // Use LinkedHashMap here to preserve the order during aggregation.
    val linkedHashMap = mutable.LinkedHashMap[String, mutable.ArrayBuffer[Transaction]]()
      .withDefault(_ => mutable.ArrayBuffer[Transaction]())
    useOldestPoints(points, Seq.empty).foreach { transaction =>
      linkedHashMap.update(transaction.payer, linkedHashMap(transaction.payer) :+ transaction)
    }
    linkedHashMap.toSeq.map {
      case (payer, arrayBuffer) => PayerPoints(payer, -arrayBuffer.map(_.points).sum)
    }
  }

  @throws[InsufficientPointsException.type]
  @tailrec
  private def useOldestPoints(points: Int, result: Seq[Transaction]): Seq[Transaction] = {
    points match {
      case 0 => result
      case _ =>
        removeOldestTransaction() match {
          case Some(transaction) =>
            if (transaction.points > points) {
              addTransaction(transaction.copy(points = transaction.points - points))
              result :+ transaction.copy(points = points)
            } else {
              useOldestPoints(points - transaction.points, result :+ transaction)
            }
          case None =>
            result.foreach(addTransaction)
            throw InsufficientPointsException
        }
    }
  }

  private def removeOldestTransaction(): Option[Transaction] = {
    if (priorityQueue.isEmpty) {
      None
    } else {
      val transaction = priorityQueue.dequeue()
      pointsPerPayer.update(transaction.payer, pointsPerPayer(transaction.payer) - transaction.points)
      Some(transaction)
    }
  }

  /**
   * Get a balance summary for each payer's total points.
   *
   * @return A map from payer to its total points.
   */
  def getPointsPerPayer: Map[String, Int] = pointsPerPayer.toMap
}
