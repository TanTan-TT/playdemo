package tag

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue
import scala.util.{Success, Failure}
import com.alicloud.openservices.tablestore.model._
import scala.concurrent.{ ExecutionContext, Future}
import play.api.libs.json.Writes

class TagRawController @Inject()(cc: TagControllerComponents)(implicit ec: ExecutionContext) 
  extends TagBaseController(cc) {

    private val logger = Logger(getClass)

    implicit val rowWrites = new Writes[Row] {
      def writes(row: Row) = {
        val rawColumnValue = row.getColumns().
                                  filter(_.getName == OtsRepo.RAW).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)

        val valueColumnValue = row.getColumns().
                                    filter(_.getName == OtsRepo.VALUE).
                                    foldLeft(0.0)(_ + _.getValue.asDouble)

        val qualityColumnValue = row.getColumns().
                                    filter(_.getName == OtsRepo.QUALITY).
                                    foldLeft(0L)(_ + _.getValue.asLong)
      
        
        Json.obj(
          "Id" -> Integer.reverse(row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.TAGID).getValue().asLong.toInt ),
          "Time" -> row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.TIME).getValue().asLong,
          "Raw" -> rawColumnValue,
          "Value" -> valueColumnValue,
          "Quality" -> qualityColumnValue.toInt
        )
      }
    }


    def get(tagId: Int, startTime: Long, endTime: Long) = Action { implicit request =>
      val tableName = config.get[String]("tagRawDataTableName")
      logger.trace(s"get tableName: $tableName")
      repo.getRawData(tableName, tagId, startTime, endTime) match {
        case Success(list) => Ok(Json.toJson(list))
        case Failure(exc) => InternalServerError(exc.getMessage)
      }
    }

    def post = Action { implicit request =>
      Ok("")
    }

    def delete = Action { implicit request => 
      Ok("")
    }
    
}

