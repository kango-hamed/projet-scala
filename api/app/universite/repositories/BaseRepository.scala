package universite.repositories

import play.api.db.Database
import anorm._
import javax.inject.Inject

// ─────────────────────────────────────────────
// Trait générique : BaseRepository
// Fournit l'accès à la base de données pour les repositories SQL
// ─────────────────────────────────────────────
trait BaseRepository {
  def db: Database

  // Exécuter une opération dans une connexion
  def withConnection[A](block: java.sql.Connection => A): A = db.withConnection(block)

  // Exécuter une opération dans une transaction
  def withTransaction[A](block: java.sql.Connection => A): A = db.withTransaction(block)
}
