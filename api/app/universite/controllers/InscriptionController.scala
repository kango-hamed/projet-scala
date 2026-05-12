package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.repositories.InscriptionRepository
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class InscriptionController @Inject()(
  cc: ControllerComponents,
  inscriptionRepo: InscriptionRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val inscriptionReads: Reads[Inscription] = Json.reads[Inscription]

  // GET /api/inscriptions
  def listerTous() = adminAction { request =>
    val liste = inscriptionRepo.toutesLesInscriptions()
    Ok(okList(liste, liste.size))
  }

  // GET /api/inscriptions/:id
  def chercher(id: String) = authAction { request =>
    inscriptionRepo.trouverParId(id) match {
      case Some(i) => 
        if (request.utilisateur.estAdmin || (request.utilisateur.estEtudiant && request.utilisateur.idProfil == i.matricule)) {
          Ok(ok(i))
        } else {
          Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit"))
        }
      case None => NotFound(notFound(s"Inscription '$id' introuvable"))
    }
  }

  // GET /api/inscriptions/etudiant/:matricule
  def parEtudiant(matricule: String) = authAction { request =>
    if (request.utilisateur.estAdmin || (request.utilisateur.estEtudiant && request.utilisateur.idProfil == matricule)) {
      val liste = inscriptionRepo.inscriptionsParEtudiant(matricule)
      Ok(okList(liste, liste.size))
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès interdit"))
    }
  }

  // GET /api/inscriptions/validees
  def validees() = adminAction { request =>
    val liste = inscriptionRepo.inscriptionsValidees()
    Ok(okList(liste, liste.size))
  }

  // GET /api/inscriptions/en-attente
  def enAttente() = adminAction { request =>
    val liste = inscriptionRepo.inscriptionsEnAttente()
    Ok(okList(liste, liste.size))
  }

  // ─── CRUD Operations ──────────────────────

  // POST /api/inscriptions → CREATE
  def creer = adminAction(parse.json) { request =>
    val idGenere = "INS-" + java.util.UUID.randomUUID().toString.substring(0, 8).toUpperCase
    val jsonBody = request.body.as[JsObject] ++ Json.obj("idInscription" -> idGenere)

    jsonBody.validate[Inscription].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide", "details" -> errors.toString)),
      inscription => {
        if (inscriptionRepo.estDejaInscrit(inscription.matricule, inscription.annee)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"L'etudiant '${inscription.matricule}' est déjà inscrit pour l'annee ${inscription.annee}"))
        } else if (inscriptionRepo.creer(inscription)) {
          Created(Json.obj("success" -> true, "message" -> "Inscription créée avec succès", "data" -> inscription))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de l'inscription"))
        }
      }
    )
  }

  // PUT /api/inscriptions/:id → UPDATE
  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Inscription].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      inscription => {
        inscriptionRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Inscription '$id' introuvable"))
          case Some(_) =>
            if (inscriptionRepo.mettreAJour(id, inscription)) {
              Ok(Json.obj("success" -> true, "message" -> "Inscription mise à jour avec succès", "data" -> inscription))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  // DELETE /api/inscriptions/:id → DELETE
  def supprimer(id: String) = adminAction { request =>
    inscriptionRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Inscription '$id' introuvable"))
      case Some(_) =>
        if (inscriptionRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Inscription '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}
