package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services._
import universite.controllers.JsonFormats._
import universite.traits._

// ──────────────────────────────────────────────
// AbsenceController
// ──────────────────────────────────────────────
@Singleton
class AbsenceController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  private val service = new AbsenceService()

  // GET /api/absences
  def toutesAbsences() = Action {
    val liste = service.toutesAbsences()
    Ok(okList(liste, liste.size))
  }

  // GET /api/absences/:matricule
  def absencesEtudiant(matricule: String) = Action {
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
  def nonJustifiees() = Action {
    val liste = service.absencesNonJustifiees()
    Ok(okList(liste, liste.size))
  }

  // GET /api/absences/a-risque
  def etudiantsARisque() = Action {
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
  def tauxGlobal() = Action {
    Ok(Json.obj(
      "success"           -> true,
      "tauxAbsenteisme"   -> service.tauxAbsenteismeGlobal(),
      "parFiliere"        -> Json.toJson(service.tauxAbsenteismeParFiliere()),
      "parMatiere"        -> Json.toJson(service.tauxAbsenteismeParMatiere())
    ))
  }

  // GET /api/absences/par-matiere
  def parMatiere() = Action {
    val rapport = service.rapportParMatiere()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> rapport.map { case (nom, total, nonJ) =>
        Json.obj("matiere" -> nom, "totalHeures" -> total, "heuresNonJustifiees" -> nonJ)
      }
    ))
  }
}

// ──────────────────────────────────────────────
// PaiementController
// ──────────────────────────────────────────────
@Singleton
class PaiementController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  private val service = new PaiementService()

  // GET /api/paiements/:matricule
  def paiementEtudiant(matricule: String) = Action {
    service.paiementEtudiant(matricule) match {
      case Some(p) => Ok(ok(p))
      case None    => NotFound(notFound(s"Aucun paiement pour '$matricule'"))
    }
  }

  // GET /api/paiements/en-dette
  def enDette() = Action {
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
  def synthese() = Action {
    Ok(Json.obj(
      "success"           -> true,
      "montantAttendu"    -> service.montantTotalAttendu(),
      "montantEncaisse"   -> service.montantTotalEncaisse(),
      "montantRestant"    -> service.montantRestant(),
      "tauxRecouvrement"  -> service.tauxRecouvrement()
    ))
  }

  // GET /api/paiements/synthese-filiere
  def syntheseParFiliere() = Action {
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
}

// ──────────────────────────────────────────────
// EnseignantController
// ──────────────────────────────────────────────
@Singleton
class EnseignantController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  private val service = new EnseignantService()

  // GET /api/enseignants
  def listerTous() = Action {
    val liste = service.tousLesEnseignants()
    Ok(okList(liste, liste.size))
  }

  // GET /api/enseignants/:id/cours
  def coursEnseignant(id: String) = Action {
    service.rechercherParId(id) match {
      case None => NotFound(notFound(s"Enseignant '$id' introuvable"))
      case Some(ens) =>
        val cours = service.coursParEnseignant(id)
        Ok(Json.obj(
          "success"    -> true,
          "enseignant" -> Json.toJson(ens),
          "cours"      -> Json.toJson(cours)
        ))
    }
  }

  // GET /api/enseignants/volumes-horaires
  def volumesHoraires() = Action {
    val volumes = service.volumeHoraireParEnseignant()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> volumes.map { case (nom, h) =>
        Json.obj("enseignant" -> nom, "volumeHoraire" -> h)
      }.toList.sortBy(-_("volumeHoraire").as[Int])
    ))
  }
}

// ──────────────────────────────────────────────
// EmploiDuTempsController
// ──────────────────────────────────────────────
@Singleton
class EmploiDuTempsController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  private val service = new EmploiDuTempsService()

  // GET /api/emploi-du-temps/filiere/:filiere
  def parFiliere(filiere: String) = Action {
    val seances = service.emploiParFiliere(filiere)
    Ok(okList(seances, seances.size))
  }

  // GET /api/emploi-du-temps/enseignant/:id
  def parEnseignant(id: String) = Action {
    val seances = service.emploiParEnseignant(id)
    Ok(okList(seances, seances.size))
  }

  // GET /api/emploi-du-temps/conflits
  def conflits() = Action {
    val conflits = service.conflitsDeSalle()
    Ok(Json.obj(
      "success"  -> true,
      "total"    -> conflits.size,
      "conflits" -> conflits.map { case (a, b) =>
        Json.obj(
          "seance1" -> Json.toJson(a),
          "seance2" -> Json.toJson(b)
        )
      }
    ))
  }
}
