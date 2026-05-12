package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.repositories._
import universite.models._
import universite.controllers.JsonFormats._
import universite.actions.{AuthAction, AdminAction}

@Singleton
class EmploiDuTempsController @Inject()(
  cc: ControllerComponents,
  seanceRepo: SeanceCoursRepository,
  authAction: AuthAction,
  adminAction: AdminAction
) extends AbstractController(cc) {

  implicit val seanceReads: Reads[SeanceCours] = Json.reads[SeanceCours]

  // GET /api/emploi-du-temps
  def listerTous() = authAction { request =>
    val liste = seanceRepo.toutesLesSeances()
    Ok(okList(liste, liste.size))
  }

  // GET /api/emploi-du-temps/filiere/:filiere
  def parFiliere(filiere: String) = authAction { request =>
    val liste = seanceRepo.parFiliere(filiere)
    Ok(okList(liste, liste.size))
  }

  // GET /api/emploi-du-temps/enseignant/:id
  def parEnseignant(id: String) = authAction { request =>
    val liste = seanceRepo.parEnseignant(id)
    Ok(okList(liste, liste.size))
  }

  // GET /api/emploi-du-temps/conflits
  def conflits() = adminAction { request =>
    val conflitsList = seanceRepo.conflitsDeSalle()
    Ok(Json.obj(
      "success" -> true,
      "total" -> conflitsList.size,
      "data" -> conflitsList.map { case (a, b) =>
        Json.obj(
          "seance1" -> Json.toJson(a),
          "seance2" -> Json.toJson(b)
        )
      }
    ))
  }

  // POST /api/emploi-du-temps
  def creer = adminAction(parse.json) { request =>
    val idGenere = "SEA-" + java.util.UUID.randomUUID().toString.substring(0, 8).toUpperCase
    val jsonBody = request.body.as[JsObject] ++ Json.obj("idSeance" -> idGenere)

    jsonBody.validate[SeanceCours].fold(
      errors => BadRequest(Json.obj("success" -> false, "erreur" -> "JSON invalide", "details" -> errors.toString)),
      seance => {
        if (seanceRepo.creer(seance)) {
          Created(Json.obj("success" -> true, "message" -> "Séance créée avec succès", "data" -> seance))
        } else {
          InternalServerError(Json.obj("success" -> false, "erreur" -> "Erreur lors de la création de la séance ou conflit"))
        }
      }
    )
  }

  // DELETE /api/emploi-du-temps/:id
  def supprimer(id: String) = adminAction { request =>
    if (seanceRepo.supprimer(id)) {
      Ok(Json.obj("success" -> true, "message" -> s"Séance '$id' supprimée avec succès"))
    } else {
      NotFound(Json.obj("success" -> false, "erreur" -> s"Séance '$id' introuvable"))
    }
  }
}
