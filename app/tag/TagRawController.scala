package tag

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json._
import scala.util.{Success, Failure}
import com.alicloud.openservices.tablestore.model.Row
import scala.concurrent.{ ExecutionContext, Future}
import play.api.libs.json.Writes

class TagRawController @Inject()(cc: TagControllerComponents)(implicit ec: ExecutionContext) 
  extends TagBaseController(cc) {

    private val logger = Logger(getClass)

    def get(tagId: Int, startTime: Long, endTime: Long) = Action { implicit request =>
      val tableName = config.get[String]("tagRawDataTableName")
      logger.trace(s"get tableName: $tableName")
      repo.getRawData(tableName, tagId, startTime, endTime) match {
        case Success(list) => Ok(list)
        case Failure(exc) => InternalServerError(exc.getMessage)
      }
    }

    def post = Action(parse.json) { implicit request: Request[JsValue] =>
      val tableName = config.get[String]("tagRawDataTableName")
      logger.trace(s"get tableName: $tableName")
      logger.info("post info")
      val jsArray: JsArray = request.body.as[JsArray]
      repo.saveRawData(tableName, jsArray) match {
        case Success(v) => Ok(v.toString)
        case Failure(exc) => Ok(exc.getMessage)
      }
    }

    def delete = Action(parse.json) { implicit request: Request[JsValue] => 
      val tableName = config.get[String]("tagRawDataTableName")
      logger.trace(s"get tableName: $tableName")
      logger.info("delete info")
      val jsArray: JsArray = request.body.as[JsArray]
      repo.deleteRawData(tableName, jsArray) match {
        case Success(v) => Ok(v.toString)
        case Failure(exc) => Ok(exc.getMessage)
      }
    }
    
}

