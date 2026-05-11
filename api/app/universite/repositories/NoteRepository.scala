package universite.repositories

import universite.models.Note
import universite.traits._

// ─────────────────────────────────────────────
// Repository : Note
// ─────────────────────────────────────────────
class NoteRepository(val cheminFichier: String = "data/notes.csv")
    extends BaseRepository[Note] {

  override def parseLigne(ligne: String): Option[Note] =
    Note.fromCSV(ligne)

  // ── Requêtes spécialisées ─────────────────

  def toutesLesNotes(): List[Note] =
    chargerOuVide()

  def notesParEtudiant(matricule: String): List[Note] =
    chargerOuVide().filter(_.matricule == matricule)

  def notesParMatiere(idMatiere: String): List[Note] =
    chargerOuVide().filter(_.idMatiere == idMatiere)

  // Notes invalides (CC ou examen hors [0,20])
  def notesInvalides(): List[Note] =
    chargerOuVide().filterNot(_.estValide)

  // Notes valides uniquement
  def notesValides(): List[Note] =
    chargerOuVide().filter(_.estValide)

  // Grouper par étudiant : Map[String, List[Note]]
  def grouperParEtudiant(): Map[String, List[Note]] =
    chargerOuVide().groupBy(_.matricule)

  // Grouper par matière
  def grouperParMatiere(): Map[String, List[Note]] =
    chargerOuVide().groupBy(_.idMatiere)

  // Étudiants ayant des notes pour une matière donnée
  def etudiantsAvecNotesPour(idMatiere: String): List[String] =
    notesParMatiere(idMatiere).map(_.matricule)

  // Vérifier si un étudiant a une note pour une matière
  def aNoteFor(matricule: String, idMatiere: String): Boolean =
    chargerOuVide().exists(n => n.matricule == matricule && n.idMatiere == idMatiere)
}
