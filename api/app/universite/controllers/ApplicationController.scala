package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.traits._

@Singleton
class ApplicationController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  // GET /api/health
  def health() = Action {
    Ok(Json.obj(
      "status"  -> "UP",
      "app"     -> "Gestion Universitaire API",
      "version" -> "1.0.0"
    ))
  }

  // OPTIONS /* — répondre aux preflight CORS
  def preflight(path: String) = Action {
    Ok("").withHeaders(
      "Access-Control-Allow-Origin"  -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS, PATCH",
      "Access-Control-Allow-Headers" -> "Content-Type, Authorization, Accept, X-Requested-With",
      "Access-Control-Expose-Headers"-> "Authorization",
      "Access-Control-Max-Age"       -> "86400"
    )
  }
}
