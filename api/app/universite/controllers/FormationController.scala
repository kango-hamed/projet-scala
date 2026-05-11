package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.FormationService
import universite.controllers.JsonFormats._

@Singleton
class FormationController @Inject()(
  cc: ControllerComponents,
  service: FormationService
) extends AbstractController(cc) {

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
}
