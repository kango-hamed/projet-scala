package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.FormationService
import universite.controllers.JsonFormats._
import universite.models._

@Singleton
class FormationController @Inject()(
  cc: ControllerComponents,
  service: FormationService,
  authAction: universite.actions.AuthAction,
  adminAction: universite.actions.AdminAction
) extends AbstractController(cc) {

  implicit val formationReads: Reads[Formation] = Json.reads[Formation]
  implicit val niveauReads: Reads[Niveau] = Json.reads[Niveau]
  implicit val semestreReads: Reads[Semestre] = Json.reads[Semestre]
  implicit val ueReads: Reads[UniteEnseignement] = Json.reads[UniteEnseignement]

  // GET /api/formations
  def listerToutes() = Action {
    val formations = service.formationRepo.toutesLesFormations()
    Ok(okList(formations, formations.size))
  }

  // GET /api/formations/:filiere/arbre
  def arbreFormation(filiere: String) = Action {
    val arbre = service.obtenirArbreFormation(filiere)
    if (arbre.isEmpty) 
      NotFound(notFound(s"Filière '$filiere' introuvable"))
    else {
      val jsonArbre = arbre.map { case (niveau, semestres) =>
        Json.obj(
          "niveau" -> Json.obj(
            "id" -> niveau.idNiveau,
            "nom" -> niveau.niveauEtudes.nom,
            "filiere" -> niveau.filiere
          ),
          "semestres" -> semestres.map { case (sem, ues) =>
            Json.obj(
              "semestre" -> Json.obj(
                "id" -> sem.idSemestre,
                "nom" -> sem.nomSemestre
              ),
              "ues" -> ues.map { ue =>
                Json.obj(
                  "id" -> ue.idUE,
                  "nom" -> ue.nomUE,
                  "coefficient" -> ue.coefficientTotal,
                  "matieres" -> ue.matieres
                )
              }
            )
          }
        )
      }
      Ok(Json.obj("success" -> true, "filiere" -> filiere, "data" -> jsonArbre))
    }
  }

  // GET /api/formations/:filiere/niveaux
  def niveauxParFiliere(filiere: String) = Action {
    val niveaux = service.formationRepo.niveauxParFiliere(filiere)
    Ok(okList(niveaux, niveaux.size))
  }

  // GET /api/formations/niveau/:id/semestres
  def semestresParNiveau(id: String) = Action {
    val niveau = service.formationRepo.trouverNiveauParId(id)
    niveau match {
      case None => NotFound(notFound(s"Niveau '$id' introuvable"))
      case Some(n) =>
        val semestres = service.formationRepo.semestresParNiveau(id)
        Ok(Json.obj(
          "success" -> true,
          "niveau" -> Json.obj("id" -> n.idNiveau, "nom" -> n.niveauEtudes.nom),
          "semestres" -> semestres,
          "total" -> semestres.size
        ))
    }
  }

  // GET /api/formations/semestre/:id/ues
  def uesParSemestre(id: String) = Action {
    val semestre = service.formationRepo.trouverSemestreParId(id)
    semestre match {
      case None => NotFound(notFound(s"Semestre '$id' introuvable"))
      case Some(s) =>
        val ues = service.formationRepo.uesParSemestre(id)
        Ok(Json.obj(
          "success" -> true,
          "semestre" -> Json.obj("id" -> s.idSemestre, "nom" -> s.nomSemestre),
          "ues" -> ues,
          "total" -> ues.size
        ))
    }
  }

  // GET /api/formations/ue/:id/matieres
  def matieresParUE(id: String) = Action {
    val ue = service.formationRepo.trouverUEParId(id)
    ue match {
      case None => NotFound(notFound(s"UE '$id' introuvable"))
      case Some(u) =>
        val matieres = service.matieresParUE(id)
        Ok(Json.obj(
          "success" -> true,
          "ue" -> Json.obj("id" -> u.idUE, "nom" -> u.nomUE, "coefficient" -> u.coefficientTotal),
          "matieres" -> matieres,
          "total" -> matieres.size
        ))
    }
  }

  // GET /api/formations/:filiere/volumes
  def volumesHoraires(filiere: String) = Action {
    val volumes = service.volumesHorairesParNiveau(filiere)
    Ok(Json.obj(
      "success" -> true,
      "filiere" -> filiere,
      "volumesParNiveau" -> Json.toJson(volumes)
    ))
  }

  // GET /api/formations/matiere-plus-difficile
  def matierePlusDifficile = Action {
    service.matiereLaPlusChargee("ALL") match {
      case None => NotFound(notFound("Aucune matière trouvée"))
      case Some(m) => Ok(Json.obj(
        "success" -> true,
        "matiere" -> Json.obj(
          "id" -> m.idMatiere,
          "nom" -> m.nomMatiere,
          "volumeHoraire" -> m.volumeHoraire,
          "coefficient" -> m.coefficient
        )
      ))
    }
  }

  // ─── CRUD Operations ──────────────────────

  // --- Formation CRUD ---
  def creerFormation = adminAction(parse.json) { request =>
    request.body.validate[Formation].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      formation => {
        if (service.formationRepo.idFormationExiste(formation.idFormation)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"La formation '${formation.idFormation}' existe déjà"))
        } else if (service.formationRepo.creerFormation(formation)) {
          Created(Json.obj("success" -> true, "message" -> "Formation créée avec succès", "data" -> formation))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de la formation"))
        }
      }
    )
  }

  def mettreAJourFormation(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Formation].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      formation => {
        service.formationRepo.trouverFormationParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Formation '$id' introuvable"))
          case Some(_) =>
            if (service.formationRepo.mettreAJourFormation(id, formation)) {
              Ok(Json.obj("success" -> true, "message" -> "Formation mise à jour avec succès", "data" -> formation))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimerFormation(id: String) = adminAction { request =>
    service.formationRepo.trouverFormationParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Formation '$id' introuvable"))
      case Some(_) =>
        if (service.formationRepo.supprimerFormation(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Formation '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }

  // --- Niveau CRUD ---
  def creerNiveau = adminAction(parse.json) { request =>
    request.body.validate[Niveau].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      niveau => {
        if (service.formationRepo.idNiveauExiste(niveau.idNiveau)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"Le niveau '${niveau.idNiveau}' existe déjà"))
        } else if (service.formationRepo.creerNiveau(niveau)) {
          Created(Json.obj("success" -> true, "message" -> "Niveau créé avec succès", "data" -> niveau))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création du niveau"))
        }
      }
    )
  }

  def mettreAJourNiveau(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Niveau].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      niveau => {
        service.formationRepo.trouverNiveauParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Niveau '$id' introuvable"))
          case Some(_) =>
            if (service.formationRepo.mettreAJourNiveau(id, niveau)) {
              Ok(Json.obj("success" -> true, "message" -> "Niveau mis à jour avec succès", "data" -> niveau))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimerNiveau(id: String) = adminAction { request =>
    service.formationRepo.trouverNiveauParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Niveau '$id' introuvable"))
      case Some(_) =>
        if (service.formationRepo.supprimerNiveau(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Niveau '$id' supprimé avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }

  // --- Semestre CRUD ---
  def creerSemestre = adminAction(parse.json) { request =>
    request.body.validate[Semestre].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      semestre => {
        if (service.formationRepo.idSemestreExiste(semestre.idSemestre)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"Le semestre '${semestre.idSemestre}' existe déjà"))
        } else if (service.formationRepo.creerSemestre(semestre)) {
          Created(Json.obj("success" -> true, "message" -> "Semestre créé avec succès", "data" -> semestre))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création du semestre"))
        }
      }
    )
  }

  def mettreAJourSemestre(id: String) = adminAction(parse.json) { request =>
    request.body.validate[Semestre].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      semestre => {
        service.formationRepo.trouverSemestreParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Semestre '$id' introuvable"))
          case Some(_) =>
            if (service.formationRepo.mettreAJourSemestre(id, semestre)) {
              Ok(Json.obj("success" -> true, "message" -> "Semestre mis à jour avec succès", "data" -> semestre))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimerSemestre(id: String) = adminAction { request =>
    service.formationRepo.trouverSemestreParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"Semestre '$id' introuvable"))
      case Some(_) =>
        if (service.formationRepo.supprimerSemestre(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"Semestre '$id' supprimé avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }

  // --- UE CRUD ---
  def creerUE = adminAction(parse.json) { request =>
    request.body.validate[UniteEnseignement].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      ue => {
        if (service.formationRepo.idUEExiste(ue.idUE)) {
          BadRequest(Json.obj("success" -> false, "erreur" -> s"L'UE '${ue.idUE}' existe déjà"))
        } else if (service.formationRepo.creerUE(ue)) {
          Created(Json.obj("success" -> true, "message" -> "UE créée avec succès", "data" -> ue))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de l'UE"))
        }
      }
    )
  }

  def mettreAJourUE(id: String) = adminAction(parse.json) { request =>
    request.body.validate[UniteEnseignement].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide")),
      ue => {
        service.formationRepo.trouverUEParId(id) match {
          case None => NotFound(Json.obj("success" -> false, "erreur" -> s"UE '$id' introuvable"))
          case Some(_) =>
            if (service.formationRepo.mettreAJourUE(id, ue)) {
              Ok(Json.obj("success" -> true, "message" -> "UE mise à jour avec succès", "data" -> ue))
            } else {
              InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la mise à jour"))
            }
        }
      }
    )
  }

  def supprimerUE(id: String) = adminAction { request =>
    service.formationRepo.trouverUEParId(id) match {
      case None => NotFound(Json.obj("success" -> false, "erreur" -> s"UE '$id' introuvable"))
      case Some(_) =>
        if (service.formationRepo.supprimerUE(id)) {
          Ok(Json.obj("success" -> true, "message" -> s"UE '$id' supprimée avec succès"))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la suppression"))
        }
    }
  }
}
