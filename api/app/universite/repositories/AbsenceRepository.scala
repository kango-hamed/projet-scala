package universite.repositories

import universite.models.Absence
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : Absence
// Gestion des absences avec PostgreSQL et Anorm
// ─────────────────────────────────────────────
@Singleton
class AbsenceRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_absence") ~
    get[String]("matricule") ~
    get[String]("id_matiere") ~
    get[String]("date_absence") ~
    get[Int]("heures") ~
    get[Boolean]("justifiee") map {
      case id ~ mat ~ matId ~ date ~ hrs ~ just =>
        Absence(id, mat, matId, date, hrs, just)
    }
  }

  def toutesLesAbsences(): List[Absence] = withConnection { implicit conn =>
    SQL"SELECT * FROM absences".as(parser.*)
  }

  def absencesParEtudiant(matricule: String): List[Absence] = withConnection { implicit conn =>
    SQL"SELECT * FROM absences WHERE matricule = $matricule".as(parser.*)
  }

  def totalHeuresParEtudiant(matricule: String): Int = withConnection { implicit conn =>
    SQL"SELECT SUM(heures) FROM absences WHERE matricule = $matricule".as(scalar[Option[Int]].single).getOrElse(0)
  }

  def absencesNonJustifiees(): List[Absence] = withConnection { implicit conn =>
    SQL"SELECT * FROM absences WHERE justifiee = false".as(parser.*)
  }

  def absencesNonJustifiees(matricule: String): List[Absence] = withConnection { implicit conn =>
    SQL"SELECT * FROM absences WHERE matricule = $matricule AND justifiee = false".as(parser.*)
  }

  def etudiantsDepassantSeuil(seuil: Int): List[(String, Int)] = withConnection { implicit conn =>
    SQL"""
      SELECT matricule, SUM(heures) as total 
      FROM absences 
      GROUP BY matricule 
      HAVING SUM(heures) > $seuil
      ORDER BY total DESC
    """.as((get[String]("matricule") ~ get[Int]("total") map { case m ~ t => (m, t) }).*)
  }

  def totalHeuresParMatiere(): Map[String, Int] = withConnection { implicit conn =>
    SQL"SELECT id_matiere, SUM(heures) as total FROM absences GROUP BY id_matiere"
      .as((get[String]("id_matiere") ~ get[Int]("total") map { case m ~ t => (m, t) }).*)
      .toMap
  }

  // ─── CRUD Operations ────────────────────────

  def creer(absence: Absence): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO absences (id_absence, matricule, id_matiere, date_absence, heures, justifiee)
      VALUES (${absence.idAbsence}, ${absence.matricule}, ${absence.idMatiere}, ${absence.date}, ${absence.heures}, ${absence.justifiee})
    """.executeUpdate() > 0
  }

  def mettreAJour(id: String, absence: Absence): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE absences SET
        matricule = ${absence.matricule},
        id_matiere = ${absence.idMatiere},
        date_absence = ${absence.date},
        heures = ${absence.heures},
        justifiee = ${absence.justifiee}
      WHERE id_absence = $id
    """.executeUpdate() > 0
  }

  def supprimer(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM absences WHERE id_absence = $id".executeUpdate() > 0
  }

  def trouverParId(id: String): Option[Absence] = withConnection { implicit conn =>
    SQL"SELECT * FROM absences WHERE id_absence = $id".as(parser.singleOpt)
  }

  def idExiste(id: String): Boolean = {
    trouverParId(id).isDefined
  }
}
