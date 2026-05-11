package universite.models

import universite.traits._
import scala.util.Try

// ─────────────────────────────────────────────
// Case class : Paiement
// Validable → vérifie que les montants sont cohérents
// ─────────────────────────────────────────────
case class Paiement(
  idPaiement   : String,
  matricule    : String,
  montantTotal : Double,
  montantPaye  : Double,
  datePaiement : String,
  mode         : String
) extends Identifiable with Affichable with Validable {

  override def id: String = idPaiement

  // Calcul du reste à payer
  def resteAPayer: Double = montantTotal - montantPaye

  def estSolde: Boolean = resteAPayer <= 0

  def tauxPaiement: Double =
    if (montantTotal == 0) 0.0
    else (montantPaye / montantTotal) * 100

  // Affichable
  override def afficher(): Unit = {
    val statut = if (estSolde) "✓ Soldé" else s"Dette: ${resteAPayer.toLong} FCFA"
    println(f"  [$idPaiement] $matricule  |  Payé: ${montantPaye.toLong}%,d / ${montantTotal.toLong}%,d FCFA  |  $statut  |  $mode")
  }

  // Validable
  override def estValide: Boolean = erreurs.isEmpty

  override def erreurs: List[String] = List(
    if (montantTotal  < 0)              Some("Montant total négatif")              else None,
    if (montantPaye   < 0)              Some("Montant payé négatif")               else None,
    if (montantPaye > montantTotal)     Some("Montant payé supérieur au total")    else None
  ).flatten
}

object Paiement {
  // id_paiement,matricule,montant_total,montant_paye,date_paiement,mode
  def fromCSV(ligne: String): Option[Paiement] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 6) None
    else for {
      total <- Try(cols(2).toDouble).toOption
      paye  <- Try(cols(3).toDouble).toOption
    } yield Paiement(cols(0), cols(1), total, paye, cols(4), cols(5))
  }
}
