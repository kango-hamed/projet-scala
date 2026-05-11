package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.actions._
import universite.services.AuthService
import universite.repositories._
import universite.models._
import universite.models.Etudiant
import universite.models.Enseignant
import universite.controllers.JsonFormats._

// ─────────────────────────────────────────────
// Controller : UtilisateurController
// Gestion des comptes utilisateurs (protégé par admin)
// ─────────────────────────────────────────────
@Singleton
class UtilisateurController @Inject()(
  cc: ControllerComponents,
  authAction: AuthAction,
  adminAction: AdminAction,
  userRepo: UtilisateurRepository,
  authService: AuthService,
  etudRepo: EtudiantRepository,
  ensRepo: EnseignantRepository
)(implicit ec: scala.concurrent.ExecutionContext)
    extends AbstractController(cc) {

  // ─── GET /api/utilisateurs ──────────────────
  // Accessible uniquement aux admins
  def listerTous = adminAction { request =>
    val utilisateurs = userRepo.tousLesUtilisateurs()
    Ok(okList(utilisateurs, utilisateurs.size))
  }

  // ─── GET /api/utilisateurs/:id ──────────────
  // Admin peut voir tous, les autres uniquement leur propre profil
  def voirProfil(id: String) = authAction { request =>
    val currentUser = request.utilisateur
    
    // Un utilisateur peut voir son propre profil, un admin peut tout voir
    val peutVoir = currentUser.estAdmin || currentUser.idUtilisateur == id
    
    if (!peutVoir) {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit à ce profil"))
    } else {
      userRepo.trouverParId(id) match {
        case None => NotFound(notFound(s"Utilisateur '$id' introuvable"))
        case Some(u) => Ok(ok(u))
      }
    }
  }

  // ─── POST /api/utilisateurs/:id/activer ─────
  def activer(id: String) = adminAction { request =>
    if (userRepo.changerStatut(id, true)) {
      Ok(Json.obj("success" -> true, "message" -> s"Utilisateur $id activé"))
    } else {
      BadRequest(Json.obj("success" -> false, "erreur" -> "Impossible d'activer l'utilisateur"))
    }
  }

  // ─── POST /api/utilisateurs/:id/suspendre ────
  def suspendre(id: String) = adminAction { request =>
    // Empêcher de suspendre son propre compte
    if (id == request.utilisateur.idUtilisateur) {
      BadRequest(Json.obj("success" -> false, "erreur" -> "Vous ne pouvez pas suspendre votre propre compte"))
    } else if (userRepo.changerStatut(id, false)) {
      Ok(Json.obj("success" -> true, "message" -> s"Utilisateur $id suspendu"))
    } else {
      BadRequest(Json.obj("success" -> false, "erreur" -> "Impossible de suspendre l'utilisateur"))
    }
  }

  // ─── GET /api/utilisateurs/profils/disponibles ──
  // Retourne les profils sans compte utilisateur
  def profilsDisponibles = adminAction { request =>
    val utilisateurs = userRepo.tousLesUtilisateurs()
    val profilsAvecCompte = utilisateurs.map(_.idProfil).toSet
    
    val etudiantsSansCompte = etudRepo.tousLesEtudiants()
      .filterNot(e => profilsAvecCompte.contains(e.matricule))
      .map(e => Json.obj("type" -> "ETUDIANT", "id" -> e.matricule, "nom" -> e.nomComplet))
    
    val enseignantsSansCompte = ensRepo.tousLesEnseignants()
      .filterNot(ens => profilsAvecCompte.contains(ens.idEnseignant))
      .map(ens => Json.obj("type" -> "ENSEIGNANT", "id" -> ens.idEnseignant, "nom" -> ens.nomComplet))
    
    val adminEntry = if (!profilsAvecCompte.contains("ADMIN")) {
      List(Json.obj("type" -> "ADMIN", "id" -> "ADMIN", "nom" -> "Administrateur Système"))
    } else List.empty
    
    val tous = adminEntry ++ enseignantsSansCompte ++ etudiantsSansCompte
    
    Ok(Json.obj(
      "success" -> true,
      "total" -> tous.size,
      "data" -> tous
    ))
  }
}
