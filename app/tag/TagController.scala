package tag

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue
import scala.util.{Success, Failure}

import scala.concurrent.{ ExecutionContext, Future}


class TagController @Inject()(cc: TagControllerComponents)(implicit ec: ExecutionContext) 
  extends TagBaseController(cc) {

    private val logger = Logger(getClass)

    def get(tagId: Long, step: Int, startTime: Long, endTime: Long) = Action { implicit request =>
      Ok("")
    }

    def post = Action { implicit request =>
      Ok("")

    }

    def delete = Action { implicit request => 
      Ok("")
    }
    
}

