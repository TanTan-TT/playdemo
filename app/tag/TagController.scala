package tag

import javax.inject.Inject

import play.api.Logger
import play.api.mvc._
import scala.util.{Success, Failure}
import play.api.libs.json._
import scala.concurrent.{ ExecutionContext, Future}


class TagController @Inject()(cc: TagControllerComponents)(implicit ec: ExecutionContext) 
  extends TagBaseController(cc) {

    private val logger = Logger(getClass)

    def get(tagId: Long, step: Int, startTime: Long, endTime: Long) = Action { implicit request =>
      val columns = request.queryString.get("columns")
      val isReverse = request.queryString.get("isreverse").map(_.map(_.toBoolean).head)
      val top = request.queryString.get("top").map(_.map(_.toInt).head)

      val tableName = config.get[String]("tagDataTableName")
      logger.trace(s"get tableName: $tableName")
      logger.trace(s"columns $columns, isReverse: $isReverse, top: $top")
      
      repo.getTagData(tableName, tagId, step, startTime, endTime, columns, isReverse, top) match {
        case Success(v) => Ok(v)
        case Failure(exc) => InternalServerError(exc.getMessage)
      }
    }

    def post = Action(parse.json) { implicit request =>
      val tableName = config.get[String]("tagDataTableName")
      logger.trace(s"get tableName: $tableName")
      logger.info("post info")
      val jsArray: JsArray = request.body.as[JsArray]
      repo.saveTagData(tableName, jsArray) match {
        case Success(v) => Ok(v.toString)
        case Failure(exc) => Ok(exc.getMessage)
      }

    }

    def delete = Action { implicit request => 
      Ok("")
    }
    
}

