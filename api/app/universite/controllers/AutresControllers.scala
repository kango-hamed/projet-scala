package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services._
import universite.controllers.JsonFormats._
import universite.traits._
import universite.models._
import universite.actions.{AuthAction, AdminAction}

// ──────────────────────────────────────────────
// AbsenceController
// ──────────────────────────────────────────────
@Singleton
class AbsenceController @Inject()(
  cc: ControllerComponents,
  service: AbsenceService,
  absenceRepo: universite.repositories.AbsenceRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val absenceReads: Reads[Absence] = Json.reads[Absence]

  // GET /api/absences
  def toutesAbsences() = authAction { request =>
    val liste = service.toutesAbsences()
    Ok(okList(liste, liste.size))
  }

  // GET /api/absences/:matricule
  def absencesEtudiant(matricule: String) = authAction { request =>
    val liste  = service.absencesParEtudiant(matricule)
    val totalH = service.totalHeuresParEtudiant(matricule)
    Ok(Json.obj(
      "success"     -> true,
      "matricule"   -> matricule,
      "totalHeures" -> totalH,
      "total"       -> liste.size,
      "data"        -> Json.toJson(liste)
    ))
  }

  // GET /api/absences/non-justifiees
  def nonJustifiees() = authAction { request =>
    val liste = service.absencesNonJustifiees()
    Ok(okList(liste, liste.size))
  }

  // GET /api/absences/a-risque
  def etudiantsARisque() = authAction { request =>
    val liste = service.etudiantsDepassantSeuil()
    Ok(Json.obj(
      "success" -> true,
      "seuil"   -> 10,
      "total"   -> liste.size,
      "data"    -> liste.map { case (m, h) =>
        Json.obj("matricule" -> m, "totalHeures" -> h)
      }
    ))
  }

  // GET /api/absences/taux-global
  def tauxGlobal() = authAction { request =>
    Ok(Json.obj(
      "success"           -> true,
      "tauxAbsenteisme"   -> service.tauxAbsenteismeGlobal(),
      "parFiliere"        -> Json.toJson(service.tauxAbsenteismeParFiliere()),
      "parMatiere"        -> Json.toJson(service.tauxAbsenteismeParMatiere())
    ))
  }

  // GET /api/absences/par-matiere
  def parMatiere() = authAction { request =>
    val rapport = service.rapportParMatiere()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> rapport.map { case (nom, total, nonJ) =>
        Json.obj("matiere" -> nom, "totalHeures" -> total, "heuresNonJustifiees" -> nonJ)
      }
    ))
  }

  // ─── CRUD Operations ──────────────────────

  def creer = adminAction(parse.json) { request =>
    request.body.validate[Absence].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      absence => {
        if (absenceRepo.idExiste(absence.idAbsence)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"L'absence '${absence.idAbsence}' existe déjà"))
        } else if (absenceRepo.creer(absence)) {
          Created(Json.obj("success" -> true, "message" -> "Absence créée avec succès", "data" -> absence))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de l'absence"))
        }
      }
    )
  }

  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Absence].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      absence => {
        absenceRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Absence '$id' introuvable"))
          case Some(_) =>
            if (absenceRepo.mettreAJour(id, absence)) {
              Ok(Json.obj("success" -> true, "message" -> "Absence mise à jour avec succès", "data" -> absence))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimer(id: String) = adminAction { request =>
    absenceRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Absence '$id' introuvable"))
      case Some(_) =>
        if (absenceRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Absence '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}

// ──────────────────────────────────────────────
// PaiementController
// ──────────────────────────────────────────────
@Singleton
class PaiementController @Inject()(
  cc: ControllerComponents,
  service: PaiementService,
  paiementRepo: universite.repositories.PaiementRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val paiementReads: Reads[Paiement] = Json.reads[Paiement]

  // GET /api/paiements/:matricule
  def paiementEtudiant(matricule: String) = authAction { request =>
    service.paiementEtudiant(matricule) match {
      case Some(p) => Ok(ok(p))
      case None    => NotFound(notFound(s"Aucun paiement pour '$matricule'"))
    }
  }

  // GET /api/paiements/en-dette
  def enDette() = authAction { request =>
    val liste = service.etudiantsEnDetteAvecNom()
    Ok(Json.obj(
      "success" -> true,
      "total"   -> liste.size,
      "data"    -> liste.map { case (mat, nom, reste) =>
        Json.obj("matricule" -> mat, "nom" -> nom, "resteAPayer" -> reste)
      }
    ))
  }

  // GET /api/paiements/synthese
  def synthese() = authAction { request =>
    Ok(Json.obj(
      "success"           -> true,
      "montantAttendu"    -> service.montantTotalAttendu(),
      "montantEncaisse"   -> service.montantTotalEncaisse(),
      "montantRestant"    -> service.montantRestant(),
      "tauxRecouvrement"  -> service.tauxRecouvrement()
    ))
  }

  // GET /api/paiements/synthese-filiere
  def syntheseParFiliere() = authAction { request =>
    val data = service.syntheseFinanciereParFiliere()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> data.map { case (filiere, (total, enc, rest)) =>
        val taux = if (total == 0) 0.0 else enc / total * 100
        Json.obj(
          "filiere"         -> filiere,
          "montantAttendu"  -> total,
          "montantEncaisse" -> enc,
          "montantRestant"  -> rest,
          "tauxRecouvrement"-> taux
        )
      }.toList
    ))
  }

  // ─── CRUD Operations ──────────────────────

  def creer = adminAction(parse.json) { request =>
    request.body.validate[Paiement].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      paiement => {
        if (paiementRepo.idExiste(paiement.idPaiement)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"Le paiement '${paiement.idPaiement}' existe déjà"))
        } else if (paiementRepo.creer(paiement)) {
          Created(Json.obj("success" -> true, "message" -> "Paiement créé avec succès", "data" -> paiement))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création du paiement"))
        }
      }
    )
  }

  def mettreAJour(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Paiement].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      paiement => {
        paiementRepo.trouverParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Paiement '$id' introuvable"))
          case Some(_) =>
            if (paiementRepo.mettreAJour(id, paiement)) {
              Ok(Json.obj("success" -> true, "message" -> "Paiement mis à jour avec succès", "data" -> paiement))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimer(id: String) = adminAction { request =>
    paiementRepo.trouverParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Paiement '$id' introuvable"))
      case Some(_) =>
        if (paiementRepo.supprimer(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Paiement '$id' supprimé avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}
