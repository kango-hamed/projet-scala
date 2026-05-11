package universite.repositories

import universite.models._
import universite.traits._

// ─────────────────────────────────────────────
// Repository : Enseignant
// ─────────────────────────────────────────────
class EnseignantRepository(val cheminFichier: String = "data/enseignants.csv")
    extends BaseRepository[Enseignant] {

  override def parseLigne(ligne: String): Option[Enseignant] =
    Enseignant.fromCSV(ligne)

  def tousLesEnseignants(): List[Enseignant] =
    chargerOuVide()

  def trouverParId(id: String): Option[Enseignant] =
    chargerOuVide().find(_.idEnseignant == id)

  def parDepartement(dept: String): List[Enseignant] =
    chargerOuVide().filter(_.departement.equalsIgnoreCase(dept))

  def departementsUniques(): Set[String] =
    chargerOuVide().map(_.departement).toSet
}

// ─────────────────────────────────────────────
// Repository : Matiere
// ─────────────────────────────────────────────
class MatiereRepository(val cheminFichier: String = "data/matieres.csv")
    extends BaseRepository[Matiere] {

  override def parseLigne(ligne: String): Option[Matiere] =
    Matiere.fromCSV(ligne)

  def toutesLesMatieres(): List[Matiere] =
    chargerOuVide()

  def trouverParId(id: String): Option[Matiere] =
    chargerOuVide().find(_.idMatiere == id)

  def parEnseignant(idEnseignant: String): List[Matiere] =
    chargerOuVide().filter(_.idEnseignant == idEnseignant)

  // Map idMatiere -> Matiere (accès O(1) depuis les services)
  def indexParId(): Map[String, Matiere] =
    chargerOuVide().map(m => m.idMatiere -> m).toMap

  // Volume horaire total d'un enseignant
  def volumeHoraireEnseignant(idEnseignant: String): Int =
    parEnseignant(idEnseignant).map(_.volumeHoraire).sum
}

// ─────────────────────────────────────────────
// Repository : Inscription
// ─────────────────────────────────────────────
class InscriptionRepository(val cheminFichier: String = "data/inscriptions.csv")
    extends BaseRepository[Inscription] {

  override def parseLigne(ligne: String): Option[Inscription] =
    Inscription.fromCSV(ligne)

  def toutesLesInscriptions(): List[Inscription] =
    chargerOuVide()

  def inscriptionsParEtudiant(matricule: String): List[Inscription] =
    chargerOuVide().filter(_.matricule == matricule)

  def inscriptionsValidees(): List[Inscription] =
    chargerOuVide().filter(_.statut == Validee)

  def inscriptionsEnAttente(): List[Inscription] =
    chargerOuVide().filter(_.statut == EnAttente)

  // Vérifier double inscription : un étudiant ne s'inscrit pas deux fois la même année
  def estDejaInscrit(matricule: String, annee: String): Boolean =
    chargerOuVide().exists(i => i.matricule == matricule && i.annee == annee)
}

// ─────────────────────────────────────────────
// Repository : SeanceCours (emplois du temps)
// ─────────────────────────────────────────────
class SeanceCoursRepository(val cheminFichier: String = "data/emplois_du_temps.csv")
    extends BaseRepository[SeanceCours] {

  override def parseLigne(ligne: String): Option[SeanceCours] =
    SeanceCours.fromCSV(ligne)

  def toutesLesSeances(): List[SeanceCours] =
    chargerOuVide()

  def parFiliere(filiere: String): List[SeanceCours] =
    chargerOuVide().filter(_.filiere.equalsIgnoreCase(filiere))

  def parEnseignant(idEnseignant: String): List[SeanceCours] =
    chargerOuVide().filter(_.idEnseignant == idEnseignant)

  def parSalle(idSalle: String): List[SeanceCours] =
    chargerOuVide().filter(_.idSalle == idSalle)

  // Détection des conflits de salle
  def conflitsDeSalle(): List[(SeanceCours, SeanceCours)] = {
    val seances = chargerOuVide()
    for {
      a <- seances
      b <- seances
      if a.idSeance < b.idSeance && a.enConflit(b)
    } yield (a, b)
  }
}

// ─────────────────────────────────────────────
// Repository : Filiere
// ─────────────────────────────────────────────
class FiliereRepository(val cheminFichier: String = "data/filieres.csv")
    extends BaseRepository[Filiere] {

  override def parseLigne(ligne: String): Option[Filiere] =
    Filiere.fromCSV(ligne)

  def toutesLesFilieres(): List[Filiere] =
    chargerOuVide()

  def trouverParNom(nom: String): Option[Filiere] =
    chargerOuVide().find(_.nomFiliere.equalsIgnoreCase(nom))
}

// ─────────────────────────────────────────────
// Repository : Salle
// ─────────────────────────────────────────────
class SalleRepository(val cheminFichier: String = "data/salles.csv")
    extends BaseRepository[Salle] {

  override def parseLigne(ligne: String): Option[Salle] =
    Salle.fromCSV(ligne)

  def toutesLesSalles(): List[Salle] =
    chargerOuVide()

  def parType(typeSalle: String): List[Salle] =
    chargerOuVide().filter(_.typeSalle.equalsIgnoreCase(typeSalle))
}
