package universite.repositories

import universite.models.Note
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : Note
// Gestion des notes avec PostgreSQL et Anorm
// ─────────────────────────────────────────────
@Singleton
class NoteRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_note") ~
    get[String]("matricule") ~
    get[String]("id_matiere") ~
    get[Double]("controle_continu") ~
    get[Double]("examen") map {
      case id ~ mat ~ matId ~ cc ~ exam =>
        Note(id, mat, matId, cc, exam)
    }
  }

  def toutesLesNotes(): List[Note] = withConnection { implicit conn =>
    SQL"SELECT * FROM notes".as(parser.*)
  }

  def notesParEtudiant(matricule: String): List[Note] = withConnection { implicit conn =>
    SQL"SELECT * FROM notes WHERE matricule = $matricule".as(parser.*)
  }

  def notesParMatiere(idMatiere: String): List[Note] = withConnection { implicit conn =>
    SQL"SELECT * FROM notes WHERE id_matiere = $idMatiere".as(parser.*)
  }

  def notesInvalides(): List[Note] = {
    toutesLesNotes().filterNot(_.estValide)
  }

  def notesValides(): List[Note] = {
    toutesLesNotes().filter(_.estValide)
  }
}
