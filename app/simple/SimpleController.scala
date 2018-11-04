package simple

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue
import scala.util.{Success, Failure}

import scala.concurrent.{ ExecutionContext, Future}


class SimpleController @Inject()(cc: SimpleControllerComponents)(implicit ec: ExecutionContext) 
  extends SimpleBaseController(cc) {

    private val logger = Logger(getClass)

    private def getMapFromQuerystringForMetadata(request: Request[AnyContent]): Map[String, String] = {
      request.queryString map {
        case (key, value) if key != "" && value.length > 0 => (key, value(0))
      }
    }
    
    def get(bucket: String, key: String): Action[AnyContent] = Action { implicit request => 
      getInternal(bucket, key)
    }

    def getWithDir(bucket: String, date: String, dir: String, key: String): Action[AnyContent] = Action { implicit request => 
      getInternal(bucket, s"$date/$dir/$key")
    }

    private def getInternal(bucket: String, key: String) = {
      repo.get(bucket, key) match {
        case Success((content, meta)) => 
          Ok(content).withHeaders(meta.toSeq: _*)  
        case Failure(e) => NotFound
      }
    }

    def getUrl(bucket: String, key: String): Action[AnyContent] = Action { implicit request => 
      val ossEndpoint = config.get[String]("ossEndpoint")
      
      Ok(repo.getUrl(ossEndpoint, bucket, key))
    }

    def getImageUrl(bucket: String, key: String): Action[AnyContent] = Action{ implicit request => 
      
      val ossEndpoint = config.get[String]("ossEndpoint")

      Ok(repo.getUrl(ossEndpoint, bucket, key, request.body.asJson))
    }

    def post(bucket: String, key: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] => 
      postInternal(bucket, key)
    }

    def postWithDir(bucket: String, date: String, dir: String, key: String): Action[AnyContent] = Action { implicit request => 
      postInternal(bucket, s"$date/$dir/$key")
    }

    private def postInternal(bucket: String, key: String)(implicit request: Request[AnyContent]) = {
      request.body.asRaw match {
        case Some(raw) if raw.size > 0 => 
          raw.asBytes(raw.memoryThreshold) match {
            case Some(byteString) if byteString.length > 0 => 
              val ret = repo.post(bucket, key, byteString.toArray[Byte], getMapFromQuerystringForMetadata(request))
              ret match {
                case Success(_) => Ok(s"")
                case Failure(e) => InternalServerError(e.getMessage())
              }
            case None => BadRequest("Body is empty")
          }
          
        case None => BadRequest("Body is empty")
      }
    }
    
}

