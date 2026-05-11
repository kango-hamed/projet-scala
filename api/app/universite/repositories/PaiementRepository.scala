package universite.repositories

import universite.models.Paiement
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : Paiement
// Gestion des paiements avec PostgreSQL et Anorm
// ─────────────────────────────────────────────
@Singleton
class PaiementRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_paiement") ~
    get[String]("matricule") ~
    get[Double]("montant_total") ~
    get[Double]("montant_paye") ~
    get[String]("date_paiement") ~
    get[String]("mode") map {
      case id ~ mat ~ total ~ paye ~ date ~ mode =>
        Paiement(id, mat, total, paye, date, mode)
    }
  }

  def tousLesPaiements(): List[Paiement] = withConnection { implicit conn =>
    SQL"SELECT * FROM paiements".as(parser.*)
  }

  def tousPaiements(): List[Paiement] = tousLesPaiements()

  def paiementParEtudiant(matricule: String): Option[Paiement] = withConnection { implicit conn =>
    SQL"SELECT * FROM paiements WHERE matricule = $matricule".as(parser.singleOpt)
  }

  def etudiantsEnDette(): List[Paiement] = withConnection { implicit conn =>
    SQL"SELECT * FROM paiements WHERE montant_paye < montant_total".as(parser.*)
  }

  def montantTotalAttendu(): Double = withConnection { implicit conn =>
    SQL"SELECT SUM(montant_total) FROM paiements".as(scalar[Option[Double]].single).getOrElse(0.0)
  }

  def montantTotalEncaisse(): Double = withConnection { implicit conn =>
    SQL"SELECT SUM(montant_paye) FROM paiements".as(scalar[Option[Double]].single).getOrElse(0.0)
  }

  def montantRestant(): Double = withConnection { implicit conn =>
    SQL"SELECT SUM(montant_total - montant_paye) FROM paiements".as(scalar[Option[Double]].single).getOrElse(0.0)
  }

  def tauxRecouvrement(): Double = {
    val total = montantTotalAttendu()
    if (total == 0) 0.0 else (montantTotalEncaisse() / total) * 100
  }

  def totalPayeRecursif(paiements: List[Paiement]): Double = paiements match {
    case Nil => 0.0
    case head :: tail => head.montantPaye + totalPayeRecursif(tail)
  }

  // ─── CRUD Operations ────────────────────────

  def creer(paiement: Paiement): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO paiements (id_paiement, matricule, montant_total, montant_paye, date_paiement, mode)
      VALUES (${paiement.idPaiement}, ${paiement.matricule}, ${paiement.montantTotal}, ${paiement.montantPaye}, ${paiement.datePaiement}, ${paiement.mode})
    """.executeUpdate() > 0
  }

  def mettreAJour(id: String, paiement: Paiement): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE paiements SET
        matricule = ${paiement.matricule},
        montant_total = ${paiement.montantTotal},
        montant_paye = ${paiement.montantPaye},
        date_paiement = ${paiement.datePaiement},
        mode = ${paiement.mode}
      WHERE id_paiement = $id
    """.executeUpdate() > 0
  }

  def supprimer(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM paiements WHERE id_paiement = $id".executeUpdate() > 0
  }

  def trouverParId(id: String): Option[Paiement] = withConnection { implicit conn =>
    SQL"SELECT * FROM paiements WHERE id_paiement = $id".as(parser.singleOpt)
  }

  def idExiste(id: String): Boolean = {
    trouverParId(id).isDefined
  }
}
