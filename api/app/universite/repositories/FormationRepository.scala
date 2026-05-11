package universite.repositories

import universite.models._
import scala.io.Source

// ─────────────────────────────────────────────
// Repository : FormationRepository
// Accès aux données des formations, niveaux, semestres et UEs
// ─────────────────────────────────────────────
class FormationRepository extends BaseRepository {

  // ─── Formations ───────────────────────────
  
  def toutesLesFormations(): List[Formation] =
    chargerCSV("../data/formations.csv", Formation.fromCSV)
  
  def trouverFormationParId(id: String): Option[Formation] =
    toutesLesFormations().find(_.idFormation == id)
  
  // ─── Niveaux ──────────────────────────────
  
  def tousLesNiveaux(): List[Niveau] =
    chargerCSV("../data/niveaux.csv", Niveau.fromCSV)
  
  def niveauxParFiliere(filiere: String): List[Niveau] =
    tousLesNiveaux().filter(_.filiere == filiere)
  
  def trouverNiveauParId(id: String): Option[Niveau] =
    tousLesNiveaux().find(_.idNiveau == id)
  
  // ─── Semestres ────────────────────────────
  
  def tousLesSemestres(): List[Semestre] =
    chargerCSV("../data/semestres.csv", Semestre.fromCSV)
  
  def semestresParNiveau(niveau: String): List[Semestre] =
    tousLesSemestres().filter(_.niveau == niveau)
  
  def trouverSemestreParId(id: String): Option[Semestre] =
    tousLesSemestres().find(_.idSemestre == id)
  
  // ─── Unités d'Enseignement ────────────────
  
  def toutesLesUEs(): List[UniteEnseignement] =
    chargerCSV("../data/ues.csv", UniteEnseignement.fromCSV)
  
  def uesParSemestre(semestre: String): List[UniteEnseignement] =
    toutesLesUEs().filter(_.semestre == semestre)
  
  def uesParFiliere(filiere: String): List[UniteEnseignement] =
    toutesLesUEs().filter(_.filiere == filiere)
  
  def trouverUEParId(id: String): Option[UniteEnseignement] =
    toutesLesUEs().find(_.idUE == id)
  
  // ─── Hiérarchie complète ──────────────────
  
  def obtenirHiearchieComplete(filiere: String): Map[String, Any] = {
    val niveaux = niveauxParFiliere(filiere)
    niveaux.map { niveau =>
      val semestres = semestresParNiveau(niveau.idNiveau)
      val semestresAvecUEs = semestres.map { sem =>
        sem -> uesParSemestre(sem.idSemestre)
      }.toMap
      niveau.nomComplet -> semestresAvecUEs
    }.toMap
  }
}
