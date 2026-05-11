package universite.dashboard

import universite.services._
import universite.repositories._
import universite.traits._

// ─────────────────────────────────────────────
// Tableau de bord académique
// Agrège tous les indicateurs décisionnels
// ─────────────────────────────────────────────
class TableauBord @javax.inject.Inject()(
  val etudService    : EtudiantService,
  val noteService    : NoteService,
  val absService     : AbsenceService,
  val paiService     : PaiementService,
  val ensService     : EnseignantService,
  val etudRepo       : EtudiantRepository,
  val matiereRepo    : MatiereRepository
) {

  // ── Indicateurs étudiants ────────────────

  def nombreTotalEtudiants(): Int =
    etudService.tousLesEtudiants().size

  def nombreParFiliere(): Map[String, Int] =
    etudService.compterParFiliere()

  def nombreParNiveau(): Map[String, Int] =
    etudService.compterParNiveau()

  // ── Indicateurs académiques ──────────────

  def moyenneGeneraleParFiliere(): Map[String, Double] = {
    etudRepo.grouperParFiliere().map { case (filiere, etudiants) =>
      val moyennes = etudiants.map(e => noteService.moyenneGenerale(e.matricule))
      val moy = if (moyennes.isEmpty) 0.0 else moyennes.sum / moyennes.size
      filiere -> moy
    }
  }

  def moyenneGeneraleParNiveau(): Map[String, Double] = {
    etudRepo.grouperParNiveau().map { case (niveau, etudiants) =>
      val moyennes = etudiants.map(e => noteService.moyenneGenerale(e.matricule))
      val moy = if (moyennes.isEmpty) 0.0 else moyennes.sum / moyennes.size
      niveau -> moy
    }
  }

  def tauxReussiteParFiliere(): Map[String, Double] = {
    etudRepo.grouperParFiliere().map { case (filiere, etudiants) =>
      val admis = etudiants.count(e => noteService.moyenneGenerale(e.matricule) >= 10.0)
      val taux  = if (etudiants.isEmpty) 0.0 else admis.toDouble / etudiants.size * 100
      filiere -> taux
    }
  }

  def tauxReussiteGlobal(): Double = {
    val tous  = etudService.tousLesEtudiants()
    val admis = tous.count(e => noteService.moyenneGenerale(e.matricule) >= 10.0)
    if (tous.isEmpty) 0.0 else admis.toDouble / tous.size * 100
  }

  def filiereAvecMeilleurTaux(): Option[(String, Double)] =
    tauxReussiteParFiliere().toList match {
      case Nil   => None
      case liste => Some(liste.maxBy(_._2))
    }

  def top5Etudiants(): List[(String, Double)] =
    noteService.topEtudiants(5)

  def etudiantsARisque(): List[String] = {
    // Étudiant à risque = moyenne < 10 OU > 10h d'absence
    val ajournes  = noteService.etudiantsAjournes().toSet
    val absents   = absService.etudiantsDepassantSeuil().map(_._1).toSet
    (ajournes ++ absents).toList.sorted
  }

  // ── Indicateurs absences ─────────────────

  def tauxAbsenteismeGlobal(): Double =
    absService.tauxAbsenteismeGlobal()

  def tauxAbsenteismeParMatiere(): Map[String, Double] =
    absService.tauxAbsenteismeParMatiere()

  // ── Indicateurs financiers ───────────────

  def montantTotalAttendu(): Double  = paiService.montantTotalAttendu()
  def montantTotalEncaisse(): Double = paiService.montantTotalEncaisse()
  def montantRestant(): Double       = paiService.montantRestant()
  def tauxRecouvrement(): Double     = paiService.tauxRecouvrement()

  // ── Indicateurs enseignants ──────────────

  def enseignantPlusCharge(): Option[(String, Int)] =
    ensService.enseignantPlusCharge()

  def matierePlusDifficile(): Option[(String, Double)] =
    noteService.matieresDifficiles().headOption

  // ─────────────────────────────────────────
  // Affichage du tableau de bord complet
  // ─────────────────────────────────────────

  def afficherComplet(): Unit = {
    println("\n")
    println("╔══════════════════════════════════════════════════════════╗")
    println("║          TABLEAU DE BORD ACADÉMIQUE — 2025-2026          ║")
    println("╚══════════════════════════════════════════════════════════╝")

    // ── Section 1 : Étudiants
    println("\n┌─ ÉTUDIANTS ─────────────────────────────────────────────")
    println(f"│  Total inscrit     : ${nombreTotalEtudiants()}%3d étudiants")
    println( "│  Par filière :")
    nombreParFiliere().toList.sortBy(_._1)
      .foreach { case (f, n) => println(f"│    $f%-22s : $n%3d") }
    println( "│  Par niveau :")
    nombreParNiveau().toList.sortBy(_._1)
      .foreach { case (n, c) => println(f"│    $n%-22s : $c%3d") }

    // ── Section 2 : Résultats académiques
    println("\n┌─ RÉSULTATS ACADÉMIQUES ─────────────────────────────────")
    println(f"│  Taux de réussite global : ${tauxReussiteGlobal()}%.1f%%")
    println( "│  Par filière :")
    tauxReussiteParFiliere().toList.sortBy(_._1)
      .foreach { case (f, t) => println(f"│    $f%-22s : $t%.1f%%") }
    println( "│  Moyenne générale par niveau :")
    moyenneGeneraleParNiveau().toList.sortBy(_._1)
      .foreach { case (n, m) => println(f"│    $n%-22s : $m%.2f/20") }


    filiereAvecMeilleurTaux() match {
      case Some((f, t)) => println(f"│  ★ Meilleure filière       : $f ($t%.1f%%)")
      case None         => ()
    }
    matierePlusDifficile() match {
      case Some((m, t)) => println(f"│  ✗ Matière la plus difficile : $m ($t%.2f/20)")
      case None         => ()
    }

    // ── Section 3 : Top 5
    println("\n┌─ TOP 5 ÉTUDIANTS ───────────────────────────────────────")
    top5Etudiants().zipWithIndex.foreach { case ((mat, moy), i) =>
      println(f"│  ${i + 1}. $mat   →   $moy%.2f/20")
    }

    // ── Section 4 : Étudiants à risque
    val risque = etudiantsARisque()
    println(s"\n┌─ ÉTUDIANTS À RISQUE (${risque.size}) ──────────────────────────")
    if (risque.isEmpty) println("│  Aucun étudiant à risque détecté.")
    else risque.foreach(m => println(s"│  $m"))

    // ── Section 5 : Absences
    println("\n┌─ ABSENCES ──────────────────────────────────────────────")
    println(f"│  Taux d'absentéisme global : ${tauxAbsenteismeGlobal()}%.1f%%")
    println( "│  Par matière :")
    tauxAbsenteismeParMatiere().toList.sortBy(-_._2)
      .foreach { case (m, t) => println(f"│    $m%-35s : $t%.1f%%") }

    // ── Section 6 : Finance
    println("\n┌─ FINANCES ──────────────────────────────────────────────")
    println(f"│  Attendu     : ${montantTotalAttendu().toLong}%,15d FCFA")
    println(f"│  Encaissé    : ${montantTotalEncaisse().toLong}%,15d FCFA")
    println(f"│  Restant     : ${montantRestant().toLong}%,15d FCFA")
    println(f"│  Recouvrement: ${tauxRecouvrement()}%.1f%%")

    // ── Section 7 : Enseignants
    println("\n┌─ ENSEIGNANTS ───────────────────────────────────────────")
    enseignantPlusCharge() match {
      case Some((nom, h)) => println(f"│  Plus grand volume horaire : $nom ($h%dh)")
      case None           => ()
    }
    println( "│  Volumes horaires :")
    ensService.volumeHoraireParEnseignant().toList.sortBy(-_._2)
      .foreach { case (nom, h) => println(f"│    $nom%-30s : $h%3dh") }


    println("\n╚══════════════════════════════════════════════════════════╝\n")
  }

  // ── Export CSV des indicateurs ───────────

  def exporterIndicateurs(cheminSortie: String = "output/statistiques/indicateurs.csv"): Unit = {
    import java.io.{File, PrintWriter}
    import scala.util.Try

    val lignes = List(
      "indicateur,valeur",
      s"total_etudiants,${nombreTotalEtudiants()}",
      s"taux_reussite_global,${f"${tauxReussiteGlobal()}%.2f"}",
      s"taux_absenteisme_global,${f"${tauxAbsenteismeGlobal()}%.2f"}",
      s"montant_total_attendu,${montantTotalAttendu().toLong}",
      s"montant_total_encaisse,${montantTotalEncaisse().toLong}",
      s"montant_restant,${montantRestant().toLong}",
      s"taux_recouvrement,${f"${tauxRecouvrement()}%.2f"}"
    ) ++ nombreParFiliere().map { case (f, n) => s"etudiants_$f,$n" } ++
      tauxReussiteParFiliere().map { case (f, t) => s"taux_reussite_$f,${f"$t%.2f"}" }

    Try {
      new File("output/statistiques").mkdirs()
      val pw = new PrintWriter(new File(cheminSortie))
      lignes.foreach(pw.println)
      pw.close()
      println(s"[OK] Indicateurs exportés → $cheminSortie")
    }.recover { case ex => println(s"[ERREUR] Export échoué : ${ex.getMessage}") }
  }
}
