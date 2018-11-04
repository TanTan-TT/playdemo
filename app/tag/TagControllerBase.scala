package tag

import javax.inject.Inject

import net.logstash.logback.marker.LogstashMarker
import play.api.{Logger, MarkerContext}
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}



/**
 * Packages up the component dependencies for the post controller.
 *
 * This is a good way to minimize the surface area exposed to the controller, so the
 * controller only has to have one thing injected.
 */
case class TagControllerComponents @Inject()(//simpleActionBuilder: SimpleActionBuilder,
                                              //  simpleResourceHandler: SimpleResourceHandler,
                                               actionBuilder: DefaultActionBuilder,
                                               parsers: PlayBodyParsers,
                                               config: Configuration,
                                               messagesApi: MessagesApi,
                                               langs: Langs,
                                               fileMimeTypes: FileMimeTypes,
                                               executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents


class TagBaseController @Inject()(cc: TagControllerComponents) extends AbstractController(cc) {

  def config = cc.config
  def repo = new OtsRepo(
                          cc.config.get[String]("otsEndpoint"), 
                          cc.config.get[String]("accessKeyId"),
                          cc.config.get[String]("accessKeySecret"),
                          cc.config.get[String]("otsInstance"))
}