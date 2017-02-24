package fly.play.ExceptionProcessor

import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader

trait ExceptionProcessor {

  def client: WSClient

  def configuration: Configuration

  def reportError(request: RequestHeader, ex: Throwable):Unit;

  def reportError(information: ErrorInformation): Unit;

}
