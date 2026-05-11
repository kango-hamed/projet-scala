package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.NoteService
import universite.models._
import universite.controllers.JsonFormats._
import universite.traits._

@Singleton
class NoteController @Inject()(
  cc: ControllerComponents,
  service: NoteService
) extends AbstractController(cc) {

  // GET /api/notes/:matricule  — relevé complet
  def releveNotes(matricule: String) = Action {
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

  // GET /api/notes/:matricule/moyenne
  def moyenneGenerale(matricule: String) = Action {
    val moy = service.moyenneGenerale(matricule)
    Ok(Json.obj(
      "success"   -> true,
      "matricule" -> matricule,
      "moyenne"   -> moy
    ))
  }

  // GET /api/notes/:matricule/decision
  def decision(matricule: String) = Action {
    val moy = service.moyenneGenerale(matricule)
    val dec = service.decisionEtudiant(matricule)
    Ok(Json.obj(
      "success"   -> true,
      "matricule" -> matricule,
      "moyenne"   -> moy,
      "decision"  -> Json.toJson(dec)
    ))
  }

  // GET /api/notes/classement
  def classement() = Action {
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
  def top5() = Action {
    val liste = service.topEtudiants(5)
    Ok(Json.obj(
      "success" -> true,
      "data"    -> liste.zipWithIndex.map { case ((m, moy), i) =>
        Json.obj("rang" -> (i + 1), "matricule" -> m, "moyenne" -> moy)
      }
    ))
  }

  // GET /api/notes/ajournes
  def ajournes() = Action {
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
  def invalides() = Action {
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
  def matieresDifficiles() = Action {
    val liste = service.matieresDifficiles()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> liste.map { case (nom, moy) =>
        Json.obj("matiere" -> nom, "moyenneClasse" -> moy)
      }
    ))
  }
}
