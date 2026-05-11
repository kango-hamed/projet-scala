package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.services.EtudiantService
import universite.controllers.JsonFormats._
import universite.traits._

@Singleton
class EtudiantController @Inject()(
  cc: ControllerComponents,
  service: EtudiantService
) extends AbstractController(cc) {

  // GET /api/etudiants
  def listerTous() = Action {
    val liste = service.tousLesEtudiants()
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/:matricule
  def chercher(matricule: String) = Action {
    service.rechercherParMatricule(matricule) match {
      case Some(e) => Ok(ok(e))
      case None    => NotFound(notFound(s"Étudiant '$matricule' introuvable"))
    }
  }

  // GET /api/etudiants/filiere/:filiere
  def parFiliere(filiere: String) = Action {
    val liste = service.parFiliere(filiere)
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/niveau/:niveau
  def parNiveau(niveau: String) = Action {
    val liste = service.parNiveau(niveau)
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/actifs
  def actifs() = Action {
    val liste = service.etudiantsActifs()
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/suspendus
  def suspendus() = Action {
    val liste = service.etudiantsSuspendus()
    Ok(okList(liste, liste.size))
  }

  // GET /api/etudiants/stats
  def statistiques() = Action {
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
  }
}
