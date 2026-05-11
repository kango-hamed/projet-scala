package universite.controllers

import play.api.libs.json._
import universite.models._

// ──────────────────────────────────────────────
// Formats JSON implicites pour tous les modèles
// Importé dans chaque controller avec :
//   import controllers.JsonFormats._
// ──────────────────────────────────────────────
object JsonFormats {

  // ── Sealed traits → String ────────────────

  implicit val statutEtudiantWrites: Writes[StatutEtudiant] =
    Writes[StatutEtudiant] {
      case Actif     => JsString("Actif")
      case Suspendu  => JsString("Suspendu")
      case Diplome   => JsString("Diplome")
      case Inconnu   => JsString("Inconnu")
    }

  implicit val decisionWrites: Writes[DecisionAcademique] =
    Writes[DecisionAcademique] {
      case Admis        => JsString("Admis")
      case Ajourne      => JsString("Ajourné")
      case Redoublement => JsString("Redoublement")
    }

  implicit val statutInscriptionWrites: Writes[StatutInscription] =
    Writes[StatutInscription] {
      case Validee   => JsString("Validée")
      case EnAttente => JsString("En attente")
      case Annulee   => JsString("Annulée")
    }

  // ── Case classes ──────────────────────────

  implicit val etudiantWrites: Writes[Etudiant] = Json.writes[Etudiant]
  implicit val enseignantWrites: Writes[Enseignant] = Json.writes[Enseignant]
  implicit val matiereWrites: Writes[Matiere] = Json.writes[Matiere]
  implicit val filiereWrites: Writes[Filiere] = Json.writes[Filiere]
  implicit val salleWrites: Writes[Salle] = Json.writes[Salle]
  implicit val noteWrites: Writes[Note] = Json.writes[Note]
  implicit val absenceWrites: Writes[Absence] = Json.writes[Absence]
  implicit val paiementWrites: Writes[Paiement] = Json.writes[Paiement]
  implicit val inscriptionWrites: Writes[Inscription] = Json.writes[Inscription]
  implicit val seanceCoursWrites: Writes[SeanceCours] = Json.writes[SeanceCours]

  // ── Formation hiérarchie ─────────────────

  implicit val niveauEtudesWrites: Writes[NiveauEtudes] =
    Writes[NiveauEtudes] { ne =>
      Json.obj(
        "nom" -> ne.nom,
        "ordre" -> ne.ordre
      )
    }

  implicit val formationWrites: Writes[Formation] = Json.writes[Formation]
  implicit val niveauWrites: Writes[Niveau] = Json.writes[Niveau]
  implicit val semestreWrites: Writes[Semestre] = Json.writes[Semestre]
  implicit val uniteEnseignementWrites: Writes[UniteEnseignement] = Json.writes[UniteEnseignement]

  // ── Helpers pour réponses standardisées ───

  def ok[A](data: A)(implicit w: Writes[A]): JsObject =
    Json.obj("success" -> true, "data" -> Json.toJson(data))

  def okList[A](data: List[A], total: Int)(implicit w: Writes[A]): JsObject =
    Json.obj("success" -> true, "total" -> total, "data" -> Json.toJson(data))

  def notFound(msg: String): JsObject =
    Json.obj("success" -> false, "erreur" -> msg)

  def erreur(msg: String): JsObject =
    Json.obj("success" -> false, "erreur" -> msg)
}
