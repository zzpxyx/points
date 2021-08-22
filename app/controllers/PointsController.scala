package controllers

import models._
import play.api.Logger
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PointsController @Inject()(cc: ControllerComponents, pointsService: PointsService)
  extends AbstractController(cc) {
  val logger: Logger = Logger(this.getClass)
  implicit val ec: ExecutionContext = cc.executionContext

  def addTransaction(): Action[JsValue] = Action.async(parse.json) { request =>
    Future.fromTry(JsResult.toTry(request.body.validate[Transaction], _ => InvalidJsonException))
      .map { transaction =>
        pointsService.addTransaction(transaction)
        NoContent
      }
      .recover(recoveryPf)
  }

  def usePoints(): Action[JsValue] = Action.async(parse.json) { request =>
    Future.fromTry(JsResult.toTry(request.body.validate[UsePointsRequest], _ => InvalidJsonException))
      .map { usePointsRequest =>
        val payerPointsSeq = pointsService.usePoints(usePointsRequest.points)
        Ok(Json.toJson(payerPointsSeq))
      }
      .recover(recoveryPf)
  }

  def getPointsPerPayer: Action[AnyContent] = Action.async {
    Future.successful[Result](Ok(Json.toJson(pointsService.getPointsPerPayer)))
  }

  private def recoveryPf: PartialFunction[Throwable, Result] = {
    case ex@InsufficientPointsException => BadRequest(ex.getMessage)
    case ex@InvalidJsonException => BadRequest(ex.getMessage)
  }
}
