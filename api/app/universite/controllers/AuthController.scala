package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.{AuthService, RateLimiterService}
import play.api.http.HttpEntity
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

// ─────────────────────────────────────────────
// Controller : AuthController
// Endpoints d'authentification (login, register, me)
// ─────────────────────────────────────────────
@Singleton
class AuthController @Inject()(
  cc: ControllerComponents,
  authService: AuthService,
  rateLimiter: RateLimiterService,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val loginReads: Reads[LoginRequest] = Json.reads[LoginRequest]
  implicit val registerReads: Reads[RegisterRequest] = Json.reads[RegisterRequest]

  // ─── POST /api/auth/login ───────────────────
  def login = Action(parse.json) { request =>
    val clientIP = request.remoteAddress
    
    // Vérifier rate limiting
    if (!rateLimiter.peutTenter(clientIP)) {
      val attente = rateLimiter.tempsAttente(clientIP)
      TooManyRequests(Json.obj(
        "success" -> false,
        "erreur" -> s"Trop de tentatives. Réessayez dans ${attente / 60} minutes."
      ))
    } else {
      request.body.validate[LoginRequest].fold(
        errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
        login => {
          authService.authentifier(login.email, login.password) match {
            case Left(error) => 
              rateLimiter.enregistrerEchec(clientIP)
              Unauthorized(Json.obj("success" -> false, "erreur" -> rateLimiter.messageErreur(clientIP)))
            case Right((utilisateur, token)) =>
              rateLimiter.enregistrerSucces(clientIP)
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
  }

  // ─── POST /api/auth/register ────────────────
  // Accessible uniquement aux admins pour créer des comptes
  def register = adminAction(parse.json) { request =>
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
  def me = authAction { request =>
    val utilisateur = request.utilisateur
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

  // ─── POST /api/auth/logout ──────────────────
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
