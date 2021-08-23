package models

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * The service for bookkeeping points. Note that this service uses a mutable priority queue.
 *
 * @param priorityQueue A mutable priority queue to store transactions.
 */
class PointsService(priorityQueue: mutable.PriorityQueue[Transaction]) {
  private val pointsPerPayer: mutable.Map[String, Int] = mutable.Map() ++ priorityQueue.toSeq.groupBy(_.payer).map {
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
    pointsPerPayer.update(transaction.payer, pointsPerPayer.getOrElse(transaction.payer, 0) + transaction.points)
  }

  /**
   * Use points from the oldest transactions.
   *
   * @param points The number of points to use.
   * @return A list showing how many points are used for each payer.
   */
  @throws[InsufficientPointsException.type]
  def usePoints(points: Int): Seq[PayerPoints] = {
    useOldestPoints(points, Seq.empty).groupBy(_.payer).toSeq.map {
      case (payer, transactions) => PayerPoints(payer, transactions.map(_.points).sum)
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
      // pointsPerPayer and priorityQueue should be synchronized, so the key should exist
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
