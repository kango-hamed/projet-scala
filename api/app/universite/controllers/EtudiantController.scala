package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.EtudiantService
import universite.repositories.EtudiantRepository
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class EtudiantController @Inject()(
  cc: ControllerComponents,
  service: EtudiantService,
  etudiantRepo: EtudiantRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val etudiantReads: Reads[Etudiant] = Json.reads[Etudiant]

  // GET /api/etudiants
  def listerTous() = authAction { request =>
    if (request.utilisateur.estAdmin) {
      val liste = service.tousLesEtudiants()
      Ok(okList(liste, liste.size))
    } else if (request.utilisateur.estEtudiant) {
      service.rechercherParMatricule(request.utilisateur.idProfil) match {
        case Some(e) => Ok(okList(List(e), 1))
        case None => NotFound(notFound(s"Étudiant '${request.utilisateur.idProfil}' introuvable"))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé"))
    }
  }

  // GET /api/etudiants/:matricule
  def chercher(matricule: String) = authAction { request =>
    if (request.utilisateur.estAdmin || (request.utilisateur.estEtudiant && request.utilisateur.idProfil == matricule)) {
      service.rechercherParMatricule(matricule) match {
        case Some(e) => Ok(ok(e))
        case None    => NotFound(notFound(s"Étudiant '$matricule' introuvable"))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé à cet étudiant"))
    }
  }

  // GET /api/etudiants/filiere/:filiere
  def parFiliere(filiere: String) = adminAction { request =>
    val liste = service.parFiliere(filiere)
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/niveau/:niveau
  def parNiveau(niveau: String) = adminAction { request =>
    val liste = service.parNiveau(niveau)
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/actifs
  def actifs() = adminAction { request =>
    val liste = service.etudiantsActifs()
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/suspendus
  def suspendus() = adminAction { request =>
    val liste = service.etudiantsSuspendus()
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/stats
  def statistiques() = authAction { request =>
    if (request.utilisateur.estAdmin) {
      val parFiliere = service.compterParFiliere()
      val parNiveau  = service.compterParNiveau()
      Ok(Json.obj(
        "success" -> true,
        "data" -> Json.obj(
          "total"      -> service.tousLesEtudiants().size,
          "actifs"     -> service.etudiantsActifs().size,
          "suspendus"  -> service.etudiantsSuspendus().size,
          "parFiliere" -> Json.toJson(parFiliere),
          "parNiveau"  -> Json.toJson(parNiveau)
        )
      ))
    } else if (request.utilisateur.estEtudiant) {
      service.rechercherParMatricule(request.utilisateur.idProfil) match {
        case Some(e) => Ok(Json.obj(
          "success" -> true,
          "data" -> Json.obj(
            "etudiant" -> e,
            "actif" -> (e.statut == Actif)
          )
        ))
        case None => NotFound(notFound(s"Étudiant '${request.utilisateur.idProfil}' introuvable"))
      }
    } else {
      Forbidden(Json.obj("success" -> false, "erreur" -> "Accès réservé"))
    }
  }

  // ─── CRUD Operations ──────────────────────

  // POST /api/etudiants → CREATE
  def creer = adminAction(parse.json) { request =>
    request.body.validate[Etudiant].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      etudiant => {
        // Vérifier si le matricule existe déjà
        if (etudiantRepo.matriculeExiste(etudiant.matricule)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"Le matricule '${etudiant.matricule}' existe déjà"))
        } else if (etudiantRepo.emailExiste(etudiant.email)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"L'email '${etudiant.email}' est déjà utilisé"))
        } else {
          if (etudiantRepo.creer(etudiant)) {
            Created(Json.obj(
              "success" -> true,
              "message" -> "Étudiant créé avec succès",
              "data" -> etudiant
            ))
          } else {
            InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de l'étudiant"))
          }
        }
      }
    )
  }

  // PUT /api/etudiants/:matricule → UPDATE
  def mettreAJour(matricule: String) = adminAction(parse.json) { request =>
    request.body.validate[Etudiant].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      etudiant => {
        // Vérifier si l'étudiant existe
        etudiantRepo.trouverParMatricule(matricule) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Étudiant '$matricule' introuvable"))
          case Some(existing) =>
            // Vérifier email unique (sauf si c'est le même)
            if (etudiant.email != existing.email && etudiantRepo.emailExiste(etudiant.email)) {
              BadRequest(Json.obj("success" -> false, "erreur" -> s"L'email '${etudiant.email}' est déjà utilisé"))
            } else {
              if (etudiantRepo.mettreAJour(matricule, etudiant)) {
                Ok(Json.obj(
                  "success" -> true,
                  "message" -> "Étudiant mis à jour avec succès",
                  "data" -> etudiant
                ))
              } else {
                InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour de l'étudiant"))
              }
            }
        }
      }
    )
  }

  // DELETE /api/etudiants/:matricule → DELETE
  def supprimer(matricule: String) = adminAction { request =>
    etudiantRepo.trouverParMatricule(matricule) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Étudiant '$matricule' introuvable"))
      case Some(_) =>
        if (etudiantRepo.supprimer(matricule)) {
          Ok(Json.obj("success" -> true, "message" -> s"Étudiant '$matricule' supprimé avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression de l'étudiant"))
        }
    }
  }
}
