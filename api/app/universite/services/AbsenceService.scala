package universite.services

import universite.models.Absence
import universite.repositories.{AbsenceRepository, EtudiantRepository, MatiereRepository}
import universite.traits._

// ─────────────────────────────────────────────
// Service : AbsenceService
// ─────────────────────────────────────────────
class AbsenceService @javax.inject.Inject()(
  val absRepo     : AbsenceRepository,
  val etudRepo    : EtudiantRepository,
  val matiereRepo : MatiereRepository
) {

  // ── Totaux ───────────────────────────────

  def toutesAbsences(): List[Absence] =
    absRepo.toutesLesAbsences()

  def absencesParEtudiant(matricule: String): List[Absence] =
    absRepo.absencesParEtudiant(matricule)

  def absencesNonJustifiees(): List[Absence] =
    absRepo.absencesNonJustifiees()

  def totalHeuresParEtudiant(matricule: String): Int =
    absRepo.totalHeuresParEtudiant(matricule)

  def totalHeuresNonJustifiees(matricule: String): Int =
    absRepo.absencesParEtudiant(matricule)
      .filterNot(_.justifiee)
      .map(_.heures)
      .sum

  // Fonction d'ordre supérieur : applique un filtre avant de sommer
  def totalHeuresAvecFiltre(matricule: String, filtre: Absence => Boolean): Int =
    absRepo.absencesParEtudiant(matricule)
      .filter(filtre)
      .map(_.heures)
      .sum

  // ── Seuils ───────────────────────────────

  val SEUIL_HEURES: Int = 10

  def etudiantsDepassantSeuil(seuil: Int = SEUIL_HEURES): List[(String, Int)] =
    absRepo.etudiantsDepassantSeuil(seuil)

  // ── Taux d'absentéisme ───────────────────

  // Taux d'absentéisme global : (heures absences / heures totales cours) * 100
  // On estime le volume total à partir des matières
  def tauxAbsenteismeGlobal(): Double = {
    val totalAbsences    = absRepo.toutesLesAbsences().map(_.heures).sum.toDouble
    val volumeTotalCours = matiereRepo.toutesLesMatieres().map(_.volumeHoraire).sum.toDouble
    if (volumeTotalCours == 0) 0.0
    else (totalAbsences / volumeTotalCours) * 100
  }

  // Taux par filière : compare les absences d'une filière au volume horaire de ses matières
  def tauxAbsenteismeParFiliere(): Map[String, Double] = {
    val etudiants     = etudRepo.grouperParFiliere()
    val volumeTotal   = matiereRepo.toutesLesMatieres().map(_.volumeHoraire).sum.toDouble
    val toutesAbsences = absRepo.toutesLesAbsences()

    etudiants.map { case (filiere, liste) =>
      val matricules = liste.map(_.matricule).toSet
      val heuresFiliere = toutesAbsences
        .filter(a => matricules.contains(a.matricule))
        .map(_.heures)
        .sum
        .toDouble
      val taux = if (volumeTotal == 0) 0.0 else (heuresFiliere / volumeTotal) * 100
      filiere -> taux
    }
  }

  // Taux par matière
  def tauxAbsenteismeParMatiere(): Map[String, Double] = {
    val matieres   = matiereRepo.indexParId()
    val parMatiere = absRepo.totalHeuresParMatiere()
    parMatiere.map { case (idMat, heures) =>
      val volume  = matieres.get(idMat).map(_.volumeHoraire.toDouble).getOrElse(1.0)
      val nomMat  = matieres.get(idMat).map(_.nomMatiere).getOrElse(idMat)
      nomMat -> (heures.toDouble / volume * 100)
    }
  }

  // ── Rapport absences ────────────────────

  def rapportParMatiere(): List[(String, Int, Int)] = {
    val matieres = matiereRepo.indexParId()
    absRepo.toutesLesAbsences()
      .groupBy(_.idMatiere)
      .map { case (idMat, absences) =>
        val nom   = matieres.get(idMat).map(_.nomMatiere).getOrElse(idMat)
        val total = absences.map(_.heures).sum
        val nonJ  = absences.filterNot(_.justifiee).map(_.heures).sum
        (nom, total, nonJ)
      }.toList
  }

  def rapportAbsencesParMatiere(): Unit = {
    val matieres = matiereRepo.indexParId()
    println("\n═══ RAPPORT ABSENCES PAR MATIÈRE ═══")
    absRepo.toutesLesAbsences()
      .groupBy(_.idMatiere)
      .foreach { case (idMat, absences) =>
        val nom   = matieres.get(idMat).map(_.nomMatiere).getOrElse(idMat)
        val total = absences.map(_.heures).sum
        val nonJ  = absences.filterNot(_.justifiee).map(_.heures).sum
        println(f"  $nom%-35s  Total: ${total}%3dh  Non justifiées: ${nonJ}%3dh")
      }
  }

  // ── Affichage console ────────────────────

  def afficherAbsencesEtudiant(matricule: String): Unit = {
    val absences = absRepo.absencesParEtudiant(matricule)
    println(s"\n═══ ABSENCES — $matricule (${totalHeuresParEtudiant(matricule)}h total) ═══")
    absences.foreach(_.afficher())
  }

  def afficherEtudiantsArisque(): Unit = {
    val liste = etudiantsDepassantSeuil()
    println(s"\n⚠  ÉTUDIANTS DÉPASSANT ${SEUIL_HEURES}h D'ABSENCE (${liste.size})")
    liste.foreach { case (mat, h) => println(s"  $mat  →  $h heures") }
  }

  def afficherTauxGlobal(): Unit = {
    val taux = tauxAbsenteismeGlobal()
    println(f"\n  Taux d'absentéisme global : $taux%.1f%%")
  }

  def afficherAbsencesNonJustifiees(): Unit = {
    val absences = absRepo.absencesNonJustifiees()
    println(s"\n═══ ABSENCES NON JUSTIFIÉES (${absences.size}) ═══")
    absences.foreach(_.afficher())
  }
}
