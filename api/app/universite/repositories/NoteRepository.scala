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

  // ─── CRUD Operations ────────────────────────

  def creer(note: Note): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO notes (id_note, matricule, id_matiere, controle_continu, examen)
      VALUES (${note.idNote}, ${note.matricule}, ${note.idMatiere}, ${note.controleContinue}, ${note.examen})
    """.executeUpdate() > 0
  }

  def mettreAJour(id: String, note: Note): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE notes SET
        matricule = ${note.matricule},
        id_matiere = ${note.idMatiere},
        controle_continu = ${note.controleContinue},
        examen = ${note.examen}
      WHERE id_note = $id
    """.executeUpdate() > 0
  }

  def supprimer(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM notes WHERE id_note = $id".executeUpdate() > 0
  }

  def trouverParId(id: String): Option[Note] = withConnection { implicit conn =>
    SQL"SELECT * FROM notes WHERE id_note = $id".as(parser.singleOpt)
  }

  def idExiste(id: String): Boolean = {
    trouverParId(id).isDefined
  }
}
