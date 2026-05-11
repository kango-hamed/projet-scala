package universite.repositories

import universite.models.{Etudiant, StatutEtudiant, Actif, Suspendu}
import universite.traits._
import scala.util.Try

// ─────────────────────────────────────────────
// Repository : Etudiant
// ─────────────────────────────────────────────
class EtudiantRepository(val cheminFichier: String = "data/etudiants.csv")
    extends BaseRepository[Etudiant] {

  override def parseLigne(ligne: String): Option[Etudiant] =
    Etudiant.fromCSV(ligne)

  // ── Requêtes spécialisées ─────────────────

  def tousLesEtudiants(): List[Etudiant] =
    chargerOuVide()

  def trouverParMatricule(matricule: String): Option[Etudiant] =
    chargerOuVide().find(_.matricule == matricule)

  def parFiliere(filiere: String): List[Etudiant] =
    chargerOuVide().filter(_.filiere.equalsIgnoreCase(filiere))

  def parNiveau(niveau: String): List[Etudiant] =
    chargerOuVide().filter(_.niveau.equalsIgnoreCase(niveau))

  def parFiliereEtNiveau(filiere: String, niveau: String): List[Etudiant] =
    chargerOuVide().filter(e =>
      e.filiere.equalsIgnoreCase(filiere) && e.niveau.equalsIgnoreCase(niveau)
    )

  def etudiantsActifs(): List[Etudiant] =
    chargerOuVide().filter(_.statut == Actif)

  def etudiantsSuspendus(): List[Etudiant] =
    chargerOuVide().filter(_.statut == Suspendu)

  // Pattern matching sur le statut
  def parStatut(statut: StatutEtudiant): List[Etudiant] =
    chargerOuVide().filter(_.statut == statut)

  // Filières uniques (utilise un Set)
  def filieresUniques(): Set[String] =
    chargerOuVide().map(_.filiere).toSet

  // Niveaux uniques
  def niveauxUniques(): Set[String] =
    chargerOuVide().map(_.niveau).toSet

  // Comptages
  def compterActifs(): Int     = etudiantsActifs().size
  def compterSuspendus(): Int  = etudiantsSuspendus().size
  def compterTotal(): Int      = chargerOuVide().size

  // Grouper par filière : Map[String, List[Etudiant]]
  def grouperParFiliere(): Map[String, List[Etudiant]] =
    chargerOuVide().groupBy(_.filiere)

  // Grouper par niveau
  def grouperParNiveau(): Map[String, List[Etudiant]] =
    chargerOuVide().groupBy(_.niveau)
}
