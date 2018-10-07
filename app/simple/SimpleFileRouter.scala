package simple

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class SimpleFileRouter @Inject()(controller: SimpleController) extends SimpleRouter {
  
  override def routes: Routes = {
    case GET(p"/$bucket/$key") => 
      controller.get(bucket, key)
    case GET(p"/$bucket/$date/$dir/$key") => 
      controller.get(bucket, date, dir, key)
    case GET(p"/geturl/$bucket/$key") => 
      controller.getUrl(bucket, key)
    case POST(p"/getimageurl/$bucket/$key") => 
      controller.getImageUrl(bucket, key)
    case POST(p"/$bucket/$key") => 
      controller.post(bucket, key)
    case PUT(p"/$bucket/$key") => 
      controller.post(bucket, key)
    case POST(p"/$bucket/$date/$dir/$key") => 
      controller.post(bucket, date, dir, key)
    case PUT(p"/$bucket/$date/$dir/$key") => 
      controller.post(bucket, date, dir, key)
  }
  
}