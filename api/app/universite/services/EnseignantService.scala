package universite.services

import universite.models._
import universite.repositories._
import universite.traits._

// ─────────────────────────────────────────────
// Service : EnseignantService
// ─────────────────────────────────────────────
class EnseignantService @javax.inject.Inject()(
  val ensRepo     : EnseignantRepository,
  val matiereRepo : MatiereRepository,
  val seanceRepo  : SeanceCoursRepository
) {

  def tousLesEnseignants(): List[Enseignant] =
    ensRepo.tousLesEnseignants()

  def rechercherParId(id: String): Option[Enseignant] =
    ensRepo.trouverParId(id)

  def parDepartement(dept: String): List[Enseignant] =
    ensRepo.parDepartement(dept)

  // Cours assurés par un enseignant
  def coursParEnseignant(idEnseignant: String): List[Matiere] =
    matiereRepo.parEnseignant(idEnseignant)

  // Volume horaire par enseignant : Map[nomEnseignant, totalHeures]
  def volumeHoraireParEnseignant(): Map[String, Int] =
    ensRepo.tousLesEnseignants().map { ens =>
      val nom    = ens.nomComplet
      val volume = matiereRepo.volumeHoraireEnseignant(ens.idEnseignant)
      nom -> volume
    }.toMap

  // Enseignant avec le plus grand volume horaire
  def enseignantPlusCharge(): Option[(String, Int)] =
    volumeHoraireParEnseignant().toList match {
      case Nil   => None
      case liste => Some(liste.maxBy(_._2))
    }

  def afficherTous(): Unit = {
    val liste = tousLesEnseignants()
    println(s"\n═══ ENSEIGNANTS (${liste.size}) ═══")
    liste.foreach(_.afficher())
  }

  def afficherCoursEnseignant(idEnseignant: String): Unit =
    rechercherParId(idEnseignant) match {
      case None      => println(s"[INFO] Enseignant $idEnseignant introuvable")
      case Some(ens) =>
        val cours = coursParEnseignant(idEnseignant)
        println(s"\n═══ COURS DE ${ens.nomComplet} ═══")
        cours.foreach(_.afficher())
    }

  def afficherVolumesHoraires(): Unit = {
    println("\n═══ VOLUME HORAIRE PAR ENSEIGNANT ═══")
    volumeHoraireParEnseignant().toList
      .sortBy(-_._2)
      .foreach { case (nom, h) => println(f"  $nom%-30s  $h%3dh") }
  }
}

// ─────────────────────────────────────────────
// Service : EmploiDuTempsService
// ─────────────────────────────────────────────
class EmploiDuTempsService @javax.inject.Inject()(
  val seanceRepo  : SeanceCoursRepository,
  val matiereRepo : MatiereRepository,
  val ensRepo     : EnseignantRepository,
  val salleRepo   : SalleRepository
) {

  def emploiParFiliere(filiere: String): List[SeanceCours] =
    seanceRepo.parFiliere(filiere)

  def emploiParEnseignant(idEnseignant: String): List[SeanceCours] =
    seanceRepo.parEnseignant(idEnseignant)

  def emploiParSalle(idSalle: String): List[SeanceCours] =
    seanceRepo.parSalle(idSalle)

  def conflitsDeSalle(): List[(SeanceCours, SeanceCours)] =
    seanceRepo.conflitsDeSalle()

  def afficherEmploiParFiliere(filiere: String): Unit = {
    val seances  = emploiParFiliere(filiere)
    val matieres = matiereRepo.indexParId()
    println(s"\n═══ EMPLOI DU TEMPS — $filiere ═══")
    println(f"  ${"Jour"}%-12s ${"Horaire"}%-14s ${"Matière"}%-35s ${"Salle"}%-10s")
    println("  " + "─" * 70)
    seances
      .sortBy(s => (s.jour, s.heureDebut))
      .foreach { s =>
        val nom = matieres.get(s.idMatiere).map(_.nomMatiere).getOrElse(s.idMatiere)
        println(f"  ${s.jour}%-12s ${s.heureDebut}-${s.heureFin}%-14s $nom%-35s ${s.idSalle}%-10s")
      }
  }

  def afficherConflits(): Unit = {
    val conflits = conflitsDeSalle()
    if (conflits.isEmpty) println("\n✓ Aucun conflit de salle détecté.")
    else {
      println(s"\n⚠  CONFLITS DE SALLE (${conflits.size})")
      conflits.foreach { case (a, b) =>
        println(s"  ${a.idSalle} — ${a.jour} ${a.heureDebut}-${a.heureFin} : ${a.idSeance} ↔ ${b.idSeance}")
      }
    }
  }
}
