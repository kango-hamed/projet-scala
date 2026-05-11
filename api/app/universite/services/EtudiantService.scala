package universite.services

import universite.models._
import universite.repositories.EtudiantRepository
import universite.traits._

// ─────────────────────────────────────────────
// Service : EtudiantService
// Logique métier sur les étudiants
// ─────────────────────────────────────────────
class EtudiantService(repo: EtudiantRepository = new EtudiantRepository()) {

  // ── Recherche ────────────────────────────

  // Option : retourne Some(etudiant) ou None
  def rechercherParMatricule(matricule: String): Option[Etudiant] =
    repo.trouverParMatricule(matricule)

  // Pattern matching sur le résultat de la recherche
  def afficherEtudiant(matricule: String): Unit =
    rechercherParMatricule(matricule) match {
      case Some(e) => e.afficher()
      case None    => println(s"[INFO] Aucun étudiant trouvé pour le matricule : $matricule")
    }

  // ── Listes et filtres ────────────────────

  def tousLesEtudiants(): List[Etudiant] =
    repo.tousLesEtudiants()

  def parFiliere(filiere: String): List[Etudiant] =
    repo.parFiliere(filiere)

  def parNiveau(niveau: String): List[Etudiant] =
    repo.parNiveau(niveau)

  def etudiantsActifs(): List[Etudiant] =
    repo.etudiantsActifs()

  def etudiantsSuspendus(): List[Etudiant] =
    repo.etudiantsSuspendus()

  // ── Statistiques ─────────────────────────

  def compterParFiliere(): Map[String, Int] =
    repo.grouperParFiliere().map { case (f, liste) => f -> liste.size }

  def compterParNiveau(): Map[String, Int] =
    repo.grouperParNiveau().map { case (n, liste) => n -> liste.size }

  // Comptage récursif (illustre la récursivité)
  def compterActifsRecursif(liste: List[Etudiant]): Int = liste match {
    case Nil => 0
    case head :: tail =>
      val point = head.statut match {
        case Actif => 1
        case _     => 0
      }
      point + compterActifsRecursif(tail)
  }

  // ── Affichage console ────────────────────

  def afficherTous(): Unit = {
    val etudiants = tousLesEtudiants()
    println(s"\n═══ LISTE DES ÉTUDIANTS (${etudiants.size}) ═══")
    etudiants.foreach(_.afficher())
  }

  def afficherParFiliere(filiere: String): Unit = {
    val liste = parFiliere(filiere)
    println(s"\n═══ ÉTUDIANTS — $filiere (${liste.size}) ═══")
    liste.foreach(e => println(s"  ${e.matricule}  ${e.nomComplet}  (${e.niveau})  [${e.statut}]"))
  }

  def afficherSuspendus(): Unit = {
    val liste = etudiantsSuspendus()
    println(s"\n⚠  ÉTUDIANTS SUSPENDUS (${liste.size})")
    liste.foreach(e => println(s"  ${e.matricule}  ${e.nomComplet}  — ${e.filiere}"))
  }

  def afficherStatistiques(): Unit = {
    println("\n═══ STATISTIQUES ÉTUDIANTS ═══")
    println(s"  Total         : ${repo.compterTotal()}")
    println(s"  Actifs        : ${repo.compterActifs()}")
    println(s"  Suspendus     : ${repo.compterSuspendus()}")
    println("\n  Par filière :")
    compterParFiliere().toList.sortBy(_._1)
      .foreach { case (f, n) => println(s"    $f : $n") }
    println("\n  Par niveau :")
    compterParNiveau().toList.sortBy(_._1)
      .foreach { case (n, c) => println(s"    $n : $c") }
  }
}
