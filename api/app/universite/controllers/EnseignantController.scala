package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.EnseignantService
import universite.repositories.EnseignantRepository
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class EnseignantController @Inject()(
  cc: ControllerComponents,
  service: EnseignantService,
  enseignantRepo: EnseignantRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val enseignantReads: Reads[Enseignant] = Json.reads[Enseignant]

  // GET /api/enseignants
  def listerTous() = authAction { request =>
    if (request.utilisateur.estAdmin) {
      val liste = service.tousLesEnseignants()
      Ok(okList(liste, liste.size))
    } else if (request.utilisateur.estEnseignant) {
      service.rechercherParId(request.utilisateur.idProfil) match {
        case Some(e) => Ok(okList(List(e), 1))
        case None => NotFound(notFound(s"Enseignant '${request.utilisateur.idProfil}' introuvable"))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé"))
    }
  }

  // GET /api/enseignants/:id
  def chercher(id: String) = authAction { request =>
    if (request.utilisateur.estAdmin || (request.utilisateur.estEnseignant && request.utilisateur.idProfil == id)) {
      service.rechercherParId(id) match {
        case Some(e) => Ok(ok(e))
        case None    => NotFound(notFound(s"Enseignant '$id' introuvable"))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé à cet enseignant"))
    }
  }

  // GET /api/enseignants/departement/:dept
  def parDepartement(dept: String) = adminAction { request =>
    val liste = service.parDepartement(dept)
    Ok(okList(liste, liste.size))
  }

  // GET /api/enseignants/volumes-horaires
  def volumesHoraires() = authAction { request =>
    if (request.utilisateur.estAdmin) {
      val volumes = service.volumeHoraireParEnseignant()
      Ok(Json.obj(
        "success" -> true,
        "data" -> Json.toJson(volumes)
      ))
    } else if (request.utilisateur.estEnseignant) {
      val h = service.volumeHoraireParEnseignant().getOrElse(request.utilisateur.idProfil, 0)
      Ok(Json.obj(
        "success" -> true,
        "enseignant" -> request.utilisateur.idProfil,
        "volumeHoraire" -> h
      ))
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé"))
    }
  }

  // GET /api/enseignants/:id/cours
  def coursEnseignant(id: String) = authAction { request =>
    if (request.utilisateur.estAdmin || (request.utilisateur.estEnseignant && request.utilisateur.idProfil == id)) {
      service.rechercherParId(id) match {
        case None => NotFound(notFound(s"Enseignant '$id' introuvable"))
        case Some(_) =>
          val cours = service.coursParEnseignant(id)
          Ok(okList(cours, cours.size))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé à cet enseignant"))
    }
  }

  // ─── CRUD Operations ──────────────────────

  // POST /api/enseignants → CREATE
  def creer = adminAction(parse.json) { request =>
    // Génération sécurisée du matricule (ex: ENS-4F2A9B1C)
    val idGenere = "ENS-" + java.util.UUID.randomUUID().toString.substring(0, 8).toUpperCase
    
    // On injecte cet ID de force dans le JSON reçu, ignorant ce que le frontend a pu envoyer
    val jsonBody = request.body.as[JsObject] ++ Json.obj("idEnseignant" -> idGenere)

    jsonBody.validate[Enseignant].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide", "details" -> errors.toString)),
      enseignant => {
        if (enseignantRepo.emailExiste(enseignant.email)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"L'email '${enseignant.email}' est déjà utilisé"))
        } else {
          if (enseignantRepo.creer(enseignant)) {
            Created(Json.obj(
              "success" -> true,
              "message" -> "Enseignant créé avec succès",
              "data" -> enseignant
            ))
          } else {
            InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de l'enseignant"))
          }
        }
      }
    )
  }

  // PUT /api/enseignants/:id → UPDATE
  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Enseignant].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      enseignant => {
        // Vérifier si l'enseignant existe
        enseignantRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Enseignant '$id' introuvable"))
          case Some(existing) =>
            // Vérifier email unique (sauf si c'est le même)
            if (enseignant.email != existing.email && enseignantRepo.emailExiste(enseignant.email)) {
              BadRequest(Json.obj("success" -> false, "erreur" -> s"L'email '${enseignant.email}' est déjà utilisé"))
            } else {
              if (enseignantRepo.mettreAJour(id, enseignant)) {
                Ok(Json.obj(
                  "success" -> true,
                  "message" -> "Enseignant mis à jour avec succès",
                  "data" -> enseignant
                ))
              } else {
                InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour de l'enseignant"))
              }
            }
        }
      }
    )
  }

  // DELETE /api/enseignants/:id → DELETE
  def supprimer(id: String) = adminAction { request =>
    enseignantRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Enseignant '$id' introuvable"))
      case Some(_) =>
        if (enseignantRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Enseignant '$id' supprimé avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression de l'enseignant"))
        }
    }
  }
}
