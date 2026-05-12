package universite.filters

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.Materializer

// ─────────────────────────────────────────────
// Filtre CORS manuel : ajoute les headers CORS
// sur TOUTES les réponses (GET, POST, etc.)
// ─────────────────────────────────────────────
class CorsFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext)
  extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (rh: RequestHeader): Future[Result] = {
    val corsHeaders = Seq(
      "Access-Control-Allow-Origin"   -> "*",
      "Access-Control-Allow-Methods"  -> "GET, POST, PUT, DELETE, OPTIONS, PATCH",
      "Access-Control-Allow-Headers"  -> "Content-Type, Authorization, Accept, X-Requested-With",
      "Access-Control-Expose-Headers" -> "Authorization"
    )
    nextFilter(rh).map(_.withHeaders(corsHeaders: _*))
  }
}
