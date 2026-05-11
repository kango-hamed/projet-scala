package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.dashboard.TableauBord
import universite.models._
import universite.controllers.JsonFormats._
import universite.traits._

@Singleton
class DashboardController @Inject()(
  cc: ControllerComponents,
  dashboard: TableauBord
) extends AbstractController(cc) {

  // GET /api/dashboard  — tous les indicateurs d'un coup
  def indicateurs() = Action {
    val top5      = dashboard.top5Etudiants()
    val risque    = dashboard.etudiantsARisque()

    Ok(Json.obj(
      "success" -> true,
      "data" -> Json.obj(

        // Étudiants
        "etudiants" -> Json.obj(
          "total"      -> dashboard.nombreTotalEtudiants(),
          "parFiliere" -> Json.toJson(dashboard.nombreParFiliere()),
          "parNiveau"  -> Json.toJson(dashboard.nombreParNiveau())
        ),

        // Académique
        "academique" -> Json.obj(
          "tauxReussiteGlobal"     -> dashboard.tauxReussiteGlobal(),
          "tauxReussiteParFiliere" -> Json.toJson(dashboard.tauxReussiteParFiliere()),
          "moyenneParFiliere"      -> Json.toJson(dashboard.moyenneGeneraleParFiliere()),
          "moyenneParNiveau"       -> Json.toJson(dashboard.moyenneGeneraleParNiveau()),
          "meilleurFiliere"        -> dashboard.filiereAvecMeilleurTaux().map { case (f, t) =>
            Json.obj("filiere" -> f, "taux" -> t)
          },
          "matierePlusDifficile"   -> dashboard.matierePlusDifficile().map { case (m, t) =>
            Json.obj("matiere" -> m, "moyenne" -> t)
          }
        ),

        // Top 5
        "top5" -> top5.zipWithIndex.map { case ((m, moy), i) =>
          Json.obj("rang" -> (i + 1), "matricule" -> m, "moyenne" -> moy)
        },

        // À risque
        "etudiantsARisque" -> risque,

        // Absences
        "absences" -> Json.obj(
          "tauxGlobal"  -> dashboard.tauxAbsenteismeGlobal(),
          "parMatiere"  -> Json.toJson(dashboard.tauxAbsenteismeParMatiere())
        ),

        // Finances
        "finances" -> Json.obj(
          "montantAttendu"   -> dashboard.montantTotalAttendu(),
          "montantEncaisse"  -> dashboard.montantTotalEncaisse(),
          "montantRestant"   -> dashboard.montantRestant(),
          "tauxRecouvrement" -> dashboard.tauxRecouvrement()
        ),

        // Enseignants
        "enseignants" -> Json.obj(
          "plusCharge" -> dashboard.enseignantPlusCharge().map { case (n, h) =>
            Json.obj("nom" -> n, "volumeHoraire" -> h)
          }
        )
      )
    ))
  }

  // GET /api/dashboard/taux-reussite
  def tauxReussite() = Action {
    Ok(Json.obj(
      "success" -> true,
      "global"  -> dashboard.tauxReussiteGlobal(),
      "data"    -> Json.toJson(dashboard.tauxReussiteParFiliere())
    ))
  }

  // GET /api/dashboard/top5
  def top5() = Action {
    val liste = dashboard.top5Etudiants()
    Ok(Json.obj(
      "success" -> true,
      "data"    -> liste.zipWithIndex.map { case ((m, moy), i) =>
        Json.obj("rang" -> (i + 1), "matricule" -> m, "moyenne" -> moy)
      }
    ))
  }

  // GET /api/dashboard/a-risque
  def etudiantsARisque() = Action {
    val liste = dashboard.etudiantsARisque()
    Ok(Json.obj("success" -> true, "total" -> liste.size, "data" -> liste))
  }

  // GET /api/dashboard/finances
  def finances() = Action {
    Ok(Json.obj(
      "success"          -> true,
      "montantAttendu"   -> dashboard.montantTotalAttendu(),
      "montantEncaisse"  -> dashboard.montantTotalEncaisse(),
      "montantRestant"   -> dashboard.montantRestant(),
      "tauxRecouvrement" -> dashboard.tauxRecouvrement()
    ))
  }
}
