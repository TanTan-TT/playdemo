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

    private def getRequestHeaderForMetadata(request: Request[AnyContent]): Map[String, String] = {
      import scala.collection.mutable.Map
      val map = Map[String, String]()
      logger.info(s"querystring ${request.queryString}")
      for((k, v) <- request.queryString if k != "" if v.length > 0) {
        map += (k -> v(0))
      }
      map.toMap
    }
    
    def get(bucket: String, key: String): Action[AnyContent] = Action { implicit request => 
      getInternal(bucket, key)
    }

    def get(bucket: String, date: String, dir: String, key: String): Action[AnyContent] = Action { implicit request => 
      getInternal(bucket, s"$date/$dir/$key")
    }

    private def getInternal(bucket: String, key: String) = {
      repo.get(bucket, key) match {
        case Left((content, meta)) => 
          Ok(content).withHeaders(meta.toSeq: _*)  
        case Right(e) => NotFound
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

    def post(bucket: String, date: String, dir: String, key: String): Action[AnyContent] = Action { implicit request => 
      postInternal(bucket, s"$date/$dir/$key")
    }

    private def postInternal(bucket: String, key: String)(implicit request: Request[AnyContent]) = {
      request.body.asRaw match {
        case Some(raw) if raw.size > 0 => 
          raw.asBytes(raw.memoryThreshold) match {
            case Some(byteString) if byteString.length > 0 => 
              val ret = repo.post(bucket, key, byteString.toArray[Byte], getRequestHeaderForMetadata(request))
              ret match {
                case Left(_) => Ok(s"")
                case Right(e) => InternalServerError(e.getMessage())
              }
            case None => BadRequest("Body is empty")
          }
          
        case None => BadRequest("Body is empty")
      }
    }
    
}

