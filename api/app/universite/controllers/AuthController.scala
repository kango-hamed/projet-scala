package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.AuthService
import universite.models._
import universite.controllers.JsonFormats._

// ─────────────────────────────────────────────
// Controller : AuthController
// Endpoints d'authentification (login, register, me)
// ─────────────────────────────────────────────
@Singleton
class AuthController @Inject()(
  cc: ControllerComponents,
  authService: AuthService
) extends AbstractController(cc) {

  implicit val loginReads: Reads[LoginRequest] = Json.reads[LoginRequest]
  implicit val registerReads: Reads[RegisterRequest] = Json.reads[RegisterRequest]

  // ─── POST /api/auth/login ───────────────────
  def login = Action(parse.json) { request =>
    request.body.validate[LoginRequest].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      login => {
        authService.authentifier(login.email, login.password) match {
          case Left(error) => Unauthorized(Json.obj("success" -> false, "erreur" -> error))
          case Right((utilisateur, token)) =>
            Ok(Json.obj(
              "success" -> true,
              "token" -> token,
              "utilisateur" -> Json.obj(
                "id" -> utilisateur.idUtilisateur,
                "email" -> utilisateur.email,
                "role" -> utilisateur.role.code,
                "roleNom" -> utilisateur.role.nom,
                "idProfil" -> utilisateur.idProfil,
                "actif" -> utilisateur.actif
              )
            ))
        }
      }
    )
  }

  // ─── POST /api/auth/register ────────────────
  // Accessible uniquement aux admins pour créer des comptes
  def register = Action(parse.json) { request =>
    request.body.validate[RegisterRequest].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      reg => {
        val role = RoleUtilisateur.fromString(reg.role)
        authService.inscrire(reg.email, reg.password, role, reg.idProfil) match {
          case Left(error) => BadRequest(Json.obj("success" -> false, "erreur" -> error))
          case Right(utilisateur) =>
            Created(Json.obj(
              "success" -> true,
              "message" -> "Compte créé avec succès",
              "utilisateur" -> Json.obj(
                "id" -> utilisateur.idUtilisateur,
                "email" -> utilisateur.email,
                "role" -> utilisateur.role.code
              )
            ))
        }
      }
    )
  }

  // ─── GET /api/auth/me ───────────────────────
  // Retourne l'utilisateur connecté depuis le token
  def me = Action { request =>
    val tokenOpt = request.headers.get("Authorization").flatMap(authService.extraireTokenHeader)
    
    authService.obtenirUtilisateurConnecte(tokenOpt) match {
      case None => Unauthorized(Json.obj("success" -> false, "erreur" -> "Token invalide ou expiré"))
      case Some(utilisateur) =>
        Ok(Json.obj(
          "success" -> true,
          "utilisateur" -> Json.obj(
            "id" -> utilisateur.idUtilisateur,
            "email" -> utilisateur.email,
            "role" -> utilisateur.role.code,
            "roleNom" -> utilisateur.role.nom,
            "idProfil" -> utilisateur.idProfil,
            "actif" -> utilisateur.actif
          )
        ))
    }
  }

  // ─── POST /api/auth/logout ──────────────────
  // Côté serveur : juste une confirmation (le token reste valide jusqu'à expiration)
  def logout = Action {
    Ok(Json.obj(
      "success" -> true,
      "message" -> "Déconnexion réussie. Supprimez le token côté client."
    ))
  }
}

// ─── Case classes pour les requêtes ───────────
case class LoginRequest(email: String, password: String)
case class RegisterRequest(email: String, password: String, role: String, idProfil: String)
