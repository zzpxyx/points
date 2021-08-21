package models

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * The service for bookkeeping points. Note that this service uses a mutable priority queue.
 *
 * @param priorityQueue A mutable priority queue to store transactions.
 */
class PointsService(priorityQueue: mutable.PriorityQueue[Transaction]) {
  /**
   * Add a transaction.
   *
   * @param transaction The transaction to be added.
   */
  def addTransaction(transaction: Transaction): Unit = {
    priorityQueue.enqueue(transaction)
  }

  /**
   * Use points from the oldest transactions.
   *
   * @param points The number of points to use.
   * @return A list showing how many points are used for each payer.
   */
  @throws[InsufficientPointsException]
  def usePoints(points: Int): Seq[PayerPoints] = {
    useOldestPoints(points, Seq.empty).groupBy(_.payer).toSeq.map {
      case (payer, transactions) => PayerPoints(payer, transactions.map(_.points).sum)
    }
  }

  @throws[InsufficientPointsException]
  @tailrec
  private def useOldestPoints(points: Int, result: Seq[Transaction]): Seq[Transaction] = {
    points match {
      case 0 => result
      case _ =>
        if (priorityQueue.isEmpty) {
          priorityQueue.enqueue(result: _*)
          throw InsufficientPointsException()
        } else {
          val transaction = priorityQueue.dequeue()
          if (transaction.points > points) {
            priorityQueue.enqueue(transaction.copy(points = transaction.points - points))
            result :+ transaction.copy(points = points)
          } else {
            useOldestPoints(points - transaction.points, result :+ transaction)
          }
        }
    }
  }

  /**
   * Get a balance summary for each payer's total points.
   *
   * @return A map from payer to its total points.
   */
  def getPointsPerPayer: Map[String, Int] = {
    priorityQueue.toSeq.groupBy(_.payer).map {
      case (payer, transactionSeq) => payer -> transactionSeq.map(_.points).sum
    }
  }
}
