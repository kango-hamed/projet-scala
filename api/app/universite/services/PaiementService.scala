package universite.services

import universite.models.Paiement
import universite.repositories.{PaiementRepository, EtudiantRepository}
import universite.traits._

// ─────────────────────────────────────────────
// Service : PaiementService
// ─────────────────────────────────────────────
class PaiementService(
  paiRepo  : PaiementRepository  = new PaiementRepository(),
  etudRepo : EtudiantRepository  = new EtudiantRepository()
) {

  // ── Recherche ────────────────────────────

  def paiementEtudiant(matricule: String): Option[Paiement] =
    paiRepo.paiementParEtudiant(matricule)

  // Pattern matching sur Option[Paiement]
  def resteAPayer(matricule: String): Double =
    paiementEtudiant(matricule) match {
      case Some(p) => p.resteAPayer
      case None    => 0.0
    }

  // ── Indicateurs financiers ───────────────

  def montantTotalAttendu(): Double  = paiRepo.montantTotalAttendu()
  def montantTotalEncaisse(): Double = paiRepo.montantTotalEncaisse()
  def montantRestant(): Double       = paiRepo.montantRestant()
  def tauxRecouvrement(): Double     = paiRepo.tauxRecouvrement()

  // Calcul récursif du total encaissé (hérite de PaiementRepository)
  def montantEncaisseRecursif(): Double =
    paiRepo.totalPayeRecursif(paiRepo.tousPaiements())

  // ── Étudiants en dette ───────────────────

  def etudiantsEnDette(): List[Paiement] =
    paiRepo.etudiantsEnDette()
      .sortBy(-_.resteAPayer)    // tri par dette décroissante

  // Jointure avec étudiants pour afficher les noms
  def etudiantsEnDetteAvecNom(): List[(String, String, Double)] = {
    val etudiants = etudRepo.tousLesEtudiants()
      .map(e => e.matricule -> e.nomComplet).toMap

    etudiantsEnDette().map { p =>
      val nom = etudiants.getOrElse(p.matricule, "Inconnu")
      (p.matricule, nom, p.resteAPayer)
    }
  }

  // ── Synthèse par filière ─────────────────

  def syntheseFinanciereParFiliere(): Map[String, (Double, Double, Double)] = {
    val parFiliere = etudRepo.grouperParFiliere()
    parFiliere.map { case (filiere, etudiants) =>
      val matricules = etudiants.map(_.matricule).toSet
      val paiements  = paiRepo.tousPaiements().filter(p => matricules.contains(p.matricule))

      val total     = paiements.map(_.montantTotal).sum
      val encaisse  = paiements.map(_.montantPaye).sum
      val restant   = paiements.map(_.resteAPayer).sum

      filiere -> (total, encaisse, restant)
    }
  }

  // ── Affichage console ────────────────────

  def afficherSyntheseGlobale(): Unit = {
    println("\n╔═══════════════════════════════════════╗")
    println("║       SYNTHÈSE FINANCIÈRE GLOBALE     ║")
    println("╠═══════════════════════════════════════╣")
    println(f"  Attendu     : ${montantTotalAttendu().toLong}%,15d FCFA")
    println(f"  Encaissé    : ${montantTotalEncaisse().toLong}%,15d FCFA")
    println(f"  Restant     : ${montantRestant().toLong}%,15d FCFA")
    println(f"  Recouvrement: ${tauxRecouvrement()}%14.1f %%")
    println("╚═══════════════════════════════════════╝")
  }

  def afficherEtudiantsEnDette(): Unit = {
    val liste = etudiantsEnDetteAvecNom()
    println(s"\n═══ ÉTUDIANTS EN RETARD DE PAIEMENT (${liste.size}) ═══")
    liste.foreach { case (mat, nom, reste) =>
      println(f"  $mat  $nom%-25s  Reste: ${reste.toLong}%,d FCFA")
    }
  }

  def afficherSyntheseParFiliere(): Unit = {
    println("\n═══ SYNTHÈSE FINANCIÈRE PAR FILIÈRE ═══")
    syntheseFinanciereParFiliere()
      .toList.sortBy(_._1)
      .foreach { case (filiere, (total, enc, rest)) =>
        val taux = if (total == 0) 0.0 else enc / total * 100
        println(f"  $filiere%-20s  Total: ${total.toLong}%,12d  Encaissé: ${enc.toLong}%,12d  Taux: $taux%.1f%%")
      }
  }

  def afficherPaiementEtudiant(matricule: String): Unit =
    paiementEtudiant(matricule) match {
      case Some(p) => p.afficher()
      case None    => println(s"[INFO] Aucun paiement trouvé pour $matricule")
    }
}
