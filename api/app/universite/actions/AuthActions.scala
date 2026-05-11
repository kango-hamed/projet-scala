package universite.actions

import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import play.api.mvc.Results.Forbidden
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import universite.services.AuthService
import universite.models._

// ─────────────────────────────────────────────
// Actions personnalisées pour l'authentification
// ─────────────────────────────────────────────

class AuthenticatedRequest[A](
  val utilisateur: Utilisateur,
  val token: String,
  request: Request[A]
) extends WrappedRequest[A](request)

@Singleton
class AuthAction @Inject()(
  val parser: BodyParsers.Default,
  val authService: AuthService
)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val tokenOpt = request.headers.get("Authorization").flatMap(authService.extraireTokenHeader)
    
    authService.obtenirUtilisateurConnecte(tokenOpt) match {
      case None =>
        Future.successful(
          Unauthorized(Json.obj("success" -> false, "erreur" -> "Token invalide ou expiré"))
        )
      case Some(utilisateur) if !utilisateur.actif =>
        Future.successful(
          Unauthorized(Json.obj("success" -> false, "erreur" -> "Compte suspendu"))
        )
      case Some(utilisateur) =>
        block(new AuthenticatedRequest(utilisateur, tokenOpt.get, request))
    }
  }
}

// ─────────────────────────────────────────────
// Actions avec vérification de rôle
// ─────────────────────────────────────────────

class AdminAction @Inject()(
  val parser: BodyParsers.Default,
  authAction: AuthAction
)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    authAction.invokeBlock(request, { (authRequest: AuthenticatedRequest[A]) =>
      if (authRequest.utilisateur.estAdmin) {
        block(authRequest)
      } else {
        Future.successful(
          Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé aux administrateurs"))
        )
      }
    })
  }
}

class EnseignantAction @Inject()(
  val parser: BodyParsers.Default,
  authAction: AuthAction
)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    authAction.invokeBlock(request, { (authRequest: AuthenticatedRequest[A]) =>
      if (authRequest.utilisateur.estAdmin || authRequest.utilisateur.estEnseignant) {
        block(authRequest)
      } else {
        Future.successful(
          Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé aux enseignants"))
        )
      }
    })
  }
}

class EtudiantAction @Inject()(
  val parser: BodyParsers.Default,
  authAction: AuthAction
)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    authAction.invokeBlock(request, { (authRequest: AuthenticatedRequest[A]) =>
      if (authRequest.utilisateur.estAdmin || authRequest.utilisateur.estEtudiant) {
        block(authRequest)
      } else {
        Future.successful(
          Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé aux étudiants"))
        )
      }
    })
  }
}
