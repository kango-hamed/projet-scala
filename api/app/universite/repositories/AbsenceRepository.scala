package universite.repositories

import universite.models.Absence
import universite.traits._

// ─────────────────────────────────────────────
// Repository : Absence
// ─────────────────────────────────────────────
class AbsenceRepository(val cheminFichier: String = "data/absences.csv")
    extends BaseRepository[Absence] {

  override def parseLigne(ligne: String): Option[Absence] =
    Absence.fromCSV(ligne)

  // ── Requêtes spécialisées ─────────────────

  def toutesLesAbsences(): List[Absence] =
    chargerOuVide()

  def absencesParEtudiant(matricule: String): List[Absence] =
    chargerOuVide().filter(_.matricule == matricule)

  def absencesParMatiere(idMatiere: String): List[Absence] =
    chargerOuVide().filter(_.idMatiere == idMatiere)

  // Absences non justifiées uniquement
  def absencesNonJustifiees(): List[Absence] =
    chargerOuVide().filterNot(_.justifiee)

  // Absences justifiées uniquement
  def absencesJustifiees(): List[Absence] =
    chargerOuVide().filter(_.justifiee)

  // Total d'heures d'absence pour un étudiant
  def totalHeuresParEtudiant(matricule: String): Int =
    absencesParEtudiant(matricule).map(_.heures).sum

  // Étudiants dépassant un seuil d'heures (par défaut 10h)
  def etudiantsDepassantSeuil(seuil: Int = 10): List[(String, Int)] = {
    val absences = chargerOuVide()
    // groupBy puis sum via foldLeft
    val totauxParEtudiant: Map[String, Int] =
      absences.foldLeft(Map.empty[String, Int]) { (acc, a) =>
        acc + (a.matricule -> (acc.getOrElse(a.matricule, 0) + a.heures))
      }
    totauxParEtudiant
      .filter { case (_, total) => total > seuil }
      .toList
      .sortBy(-_._2)
  }

  // Total heures par matière
  def totalHeuresParMatiere(): Map[String, Int] =
    chargerOuVide()
      .groupBy(_.idMatiere)
      .map { case (mat, abs) => mat -> abs.map(_.heures).sum }
}
