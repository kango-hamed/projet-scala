package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.NoteService
import universite.models._
import universite.controllers.JsonFormats._
import universite.traits._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class NoteController @Inject()(
  cc: ControllerComponents,
  service: NoteService,
  noteRepo: universite.repositories.NoteRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val noteReads: Reads[Note] = Json.reads[Note]

  // GET /api/notes/:matricule  — relevé complet
  def releveNotes(matricule: String) = authAction { request =>
    // Un étudiant ne peut voir que ses propres notes
    if (request.utilisateur.estEtudiant && request.utilisateur.idProfil != matricule) {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit aux notes d'autrui"))
    } else {
      val notes = service.noteService_notesParEtudiant(matricule)
      if (notes.isEmpty)
        NotFound(notFound(s"Aucune note pour '$matricule'"))
      else {
        val moy = service.moyenneGenerale(matricule)
        val dec = service.decisionEtudiant(matricule)
        Ok(Json.obj(
          "success"   -> true,
          "matricule" -> matricule,
          "notes"     -> Json.toJson(notes),
          "moyenneGenerale" -> moy,
          "decision"  -> Json.toJson(dec)
        ))
      }
    }
  }

  // GET /api/notes/:matricule/moyenne
  def moyenneGenerale(matricule: String) = authAction { request =>
    if (request.utilisateur.estEtudiant && request.utilisateur.idProfil != matricule) {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit"))
    } else {
      val moy = service.moyenneGenerale(matricule)
      Ok(Json.obj(
        "success"   -> true,
        "matricule" -> matricule,
        "moyenne"   -> moy
      ))
    }
  }

  // GET /api/notes/:matricule/decision
  def decision(matricule: String) = authAction { request =>
    if (request.utilisateur.estEtudiant && request.utilisateur.idProfil != matricule) {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit"))
    } else {
      val moy = service.moyenneGenerale(matricule)
      val dec = service.decisionEtudiant(matricule)
      Ok(Json.obj(
        "success"   -> true,
        "matricule" -> matricule,
        "moyenne"   -> moy,
        "decision"  -> Json.toJson(dec)
      ))
    }
  }

  // GET /api/notes/classement
  def classement() = adminAction { request =>
    val liste = service.classementEtudiants()
    Ok(Json.obj(
      "success" -> true,
      "total"   -> liste.size,
      "data"    -> liste.map { case (m, moy) =>
        Json.obj("matricule" -> m, "moyenne" -> moy)
      }
    ))
  }

  // GET /api/notes/top5
  def top5() = authAction { request =>
    val liste = service.topEtudiants(5)
    Ok(Json.obj(
      "success" -> true,
      "data"    -> liste.zipWithIndex.map { case ((m, moy), i) =>
        Json.obj("rang" -> (i + 1), "matricule" -> m, "moyenne" -> moy)
      }
    ))
  }

  // GET /api/notes/ajournes
  def ajournes() = adminAction { request =>
    val liste = service.etudiantsAjournes()
    Ok(Json.obj(
      "success" -> true,
      "total"   -> liste.size,
      "data"    -> liste.map { m =>
        Json.obj(
          "matricule" -> m,
          "moyenne"   -> service.moyenneGenerale(m),
          "decision"  -> Json.toJson(service.decisionEtudiant(m))
        )
      }
    ))
  }

  // GET /api/notes/invalides
  def invalides() = adminAction { request =>
    val notes = service.notesInvalides()
    Ok(Json.obj(
      "success" -> true,
      "total"   -> notes.size,
      "data"    -> notes.map { n =>
        Json.obj(
          "idNote"   -> n.idNote,
          "matricule" -> n.matricule,
          "matiere"  -> n.idMatiere,
          "erreurs"  -> n.erreurs
        )
      }
    ))
  }

  // GET /api/notes/matieres-difficiles
  def matieresDifficiles() = authAction { request =>
    val liste = service.matieresDifficiles()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> liste.map { case (nom, moy) =>
        Json.obj("matiere" -> nom, "moyenneClasse" -> moy)
      }
    ))
  }

  // ─── CRUD Operations ──────────────────────

  // POST /api/notes → CREATE
  def creer = adminAction(parse.json) { request =>
    val idGenere = "NOT-" + java.util.UUID.randomUUID().toString.substring(0, 8).toUpperCase
    val jsonBody = request.body.as[JsObject] ++ Json.obj("idNote" -> idGenere)

    jsonBody.validate[Note].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide", "details" -> errors.toString)),
      note => {
        if (noteRepo.creer(note)) {
          Created(Json.obj("success" -> true, "message" -> "Note créée avec succès", "data" -> note))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de la note"))
        }
      }
    )
  }

  // PUT /api/notes/:id → UPDATE
  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Note].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      note => {
        noteRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Note '$id' introuvable"))
          case Some(_) =>
            if (noteRepo.mettreAJour(id, note)) {
              Ok(Json.obj("success" -> true, "message" -> "Note mise à jour avec succès", "data" -> note))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  // DELETE /api/notes/:id → DELETE
  def supprimer(id: String) = adminAction { request =>
    noteRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Note '$id' introuvable"))
      case Some(_) =>
        if (noteRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Note '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}
