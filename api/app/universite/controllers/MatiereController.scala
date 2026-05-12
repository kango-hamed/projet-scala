package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.repositories.MatiereRepository
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class MatiereController @Inject()(
  cc: ControllerComponents,
  matiereRepo: MatiereRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val matiereReads: Reads[Matiere] = Json.reads[Matiere]

  // GET /api/matieres
  def listerTous() = authAction { request =>
    val liste = matiereRepo.toutesLesMatieres()
    Ok(okList(liste, liste.size))
  }

  // GET /api/matieres/:id
  def chercher(id: String) = authAction { request =>
    matiereRepo.trouverParId(id) match {
      case Some(m) => Ok(ok(m))
      case None    => NotFound(notFound(s"Matiere '$id' introuvable"))
    }
  }

  // GET /api/matieres/enseignant/:id
  def parEnseignant(id: String) = authAction { request =>
    if (request.utilisateur.estAdmin || (request.utilisateur.estEnseignant && request.utilisateur.idProfil == id)) {
      val liste = matiereRepo.parEnseignant(id)
      Ok(okList(liste, liste.size))
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit"))
    }
  }

  // ─── CRUD Operations ──────────────────────

  // POST /api/matieres → CREATE
  def creer = adminAction(parse.json) { request =>
    request.body.validate[Matiere].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> s"JSON invalide: $errors")),
      matiere => {
        if (matiereRepo.idExiste(matiere.idMatiere)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"La matiere '${matiere.idMatiere}' existe déjà"))
        } else if (matiereRepo.creer(matiere)) {
          Created(Json.obj("success" -> true, "message" -> "Matiere créée avec succès", "data" -> matiere))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de la matiere"))
        }
      }
    )
  }

  // PUT /api/matieres/:id → UPDATE
  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Matiere].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> s"JSON invalide: $errors")),
      matiere => {
        matiereRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Matiere '$id' introuvable"))
          case Some(_) =>
            if (matiereRepo.mettreAJour(id, matiere)) {
              Ok(Json.obj("success" -> true, "message" -> "Matiere mise à jour avec succès", "data" -> matiere))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  // DELETE /api/matieres/:id → DELETE
  def supprimer(id: String) = adminAction { request =>
    matiereRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Matiere '$id' introuvable"))
      case Some(_) =>
        if (matiereRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Matiere '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}
