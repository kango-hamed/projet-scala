package universite.repositories

import universite.models.Paiement
import universite.traits._

// ─────────────────────────────────────────────
// Repository : Paiement
// ─────────────────────────────────────────────
class PaiementRepository(val cheminFichier: String = "data/paiements.csv")
    extends BaseRepository[Paiement] {

  override def parseLigne(ligne: String): Option[Paiement] =
    Paiement.fromCSV(ligne)

  // ── Requêtes spécialisées ─────────────────

  def tousPaiements(): List[Paiement] =
    chargerOuVide()

  def paiementParEtudiant(matricule: String): Option[Paiement] =
    chargerOuVide().find(_.matricule == matricule)

  // Étudiants avec une dette (reste à payer > 0)
  def etudiantsEnDette(): List[Paiement] =
    chargerOuVide().filterNot(_.estSolde)

  // Étudiants totalement soldés
  def etudiantsSoldes(): List[Paiement] =
    chargerOuVide().filter(_.estSolde)

  // Montant total attendu (somme de tous les montantTotal)
  def montantTotalAttendu(): Double =
    chargerOuVide().map(_.montantTotal).sum

  // Montant total encaissé (somme de tous les montantPaye)
  def montantTotalEncaisse(): Double =
    chargerOuVide().map(_.montantPaye).sum

  // Montant restant à recouvrer
  def montantRestant(): Double =
    chargerOuVide().map(_.resteAPayer).sum

  // Taux global de recouvrement (en %)
  def tauxRecouvrement(): Double = {
    val total = montantTotalAttendu()
    if (total == 0) 0.0
    else (montantTotalEncaisse() / total) * 100
  }

  // Paiements par mode (Mobile Money / Banque)
  def grouperParMode(): Map[String, List[Paiement]] =
    chargerOuVide().groupBy(_.mode)

  // Récursif : calcul du total de paiements payés
  def totalPayeRecursif(paiements: List[Paiement]): Double =
    paiements match {
      case Nil         => 0.0
      case head :: tail => head.montantPaye + totalPayeRecursif(tail)
    }
}
