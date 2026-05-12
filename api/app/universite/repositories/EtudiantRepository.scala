package universite.repositories

import universite.models.{Etudiant, StatutEtudiant}
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : Etudiant
// Gestion des étudiants avec PostgreSQL et Anorm
// ─────────────────────────────────────────────
@Singleton
class EtudiantRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("matricule") ~
    get[String]("nom") ~
    get[String]("prenom") ~
    get[String]("sexe") ~
    get[String]("date_naissance") ~
    get[String]("email") ~
    get[String]("telephone") ~
    get[String]("filiere") ~
    get[String]("niveau") ~
    get[String]("annee") ~
    get[String]("statut") map {
      case mat ~ nom ~ prenom ~ sexe ~ dn ~ email ~ tel ~ fil ~ niv ~ an ~ statut =>
        Etudiant(mat, nom, prenom, sexe, dn, email, tel, fil, niv, an, StatutEtudiant.fromString(statut))
    }
  }

  def tousLesEtudiants(): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants".as(parser.*)
  }

  def trouverParMatricule(matricule: String): Option[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE matricule = $matricule".as(parser.singleOpt)
  }

  def parFiliere(filiere: String): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE filiere = $filiere".as(parser.*)
  }

  def parNiveau(niveau: String): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE niveau = $niveau".as(parser.*)
  }

  def parFiliereEtNiveau(filiere: String, niveau: String): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE filiere = $filiere AND niveau = $niveau".as(parser.*)
  }

  def etudiantsActifs(): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE statut = 'actif'".as(parser.*)
  }

  def etudiantsSuspendus(): List[Etudiant] = withConnection { implicit conn =>
    SQL"SELECT * FROM etudiants WHERE statut = 'suspendu'".as(parser.*)
  }

  def parStatut(statut: StatutEtudiant): List[Etudiant] = withConnection { implicit conn =>
    val statutStr = statut.toString.toLowerCase
    SQL"SELECT * FROM etudiants WHERE statut = $statutStr".as(parser.*)
  }

  def filieresUniques(): Set[String] = withConnection { implicit conn =>
    SQL"SELECT DISTINCT filiere FROM etudiants".as(scalar[String].*).toSet
  }

  def niveauxUniques(): Set[String] = withConnection { implicit conn =>
    SQL"SELECT DISTINCT niveau FROM etudiants".as(scalar[String].*).toSet
  }

  def compterActifs(): Int = withConnection { implicit conn =>
    SQL"SELECT COUNT(*) FROM etudiants WHERE statut = 'actif'".as(scalar[Long].single).toInt
  }

  def compterSuspendus(): Int = withConnection { implicit conn =>
    SQL"SELECT COUNT(*) FROM etudiants WHERE statut = 'suspendu'".as(scalar[Long].single).toInt
  }

  def compterTotal(): Int = withConnection { implicit conn =>
    SQL"SELECT COUNT(*) FROM etudiants".as(scalar[Long].single).toInt
  }

  def grouperParFiliere(): Map[String, List[Etudiant]] = {
    tousLesEtudiants().groupBy(_.filiere)
  }

  def grouperParNiveau(): Map[String, List[Etudiant]] = {
    tousLesEtudiants().groupBy(_.niveau)
  }

  // ─── CRUD Operations ────────────────────────

  def creer(etudiant: Etudiant): Boolean = {
    scala.util.Try {
      withConnection { implicit conn =>
        SQL"""
          INSERT INTO etudiants (matricule, nom, prenom, sexe, date_naissance, email, telephone, filiere, niveau, annee, statut)
          VALUES (${etudiant.matricule}, ${etudiant.nom}, ${etudiant.prenom}, ${etudiant.sexe}, ${etudiant.dateNaissance}, ${etudiant.email}, ${etudiant.telephone}, ${etudiant.filiere}, ${etudiant.niveau}, ${etudiant.annee}, ${etudiant.statut.toString.toLowerCase})
        """.executeUpdate() > 0
      }
    }.getOrElse(false)
  }

  def mettreAJour(matricule: String, etudiant: Etudiant): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE etudiants SET
        nom = ${etudiant.nom},
        prenom = ${etudiant.prenom},
        sexe = ${etudiant.sexe},
        date_naissance = ${etudiant.dateNaissance},
        email = ${etudiant.email},
        telephone = ${etudiant.telephone},
        filiere = ${etudiant.filiere},
        niveau = ${etudiant.niveau},
        annee = ${etudiant.annee},
        statut = ${etudiant.statut.toString.toLowerCase}
      WHERE matricule = $matricule
    """.executeUpdate() > 0
  }

  def supprimer(matricule: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM etudiants WHERE matricule = $matricule".executeUpdate() > 0
  }

  def matriculeExiste(matricule: String): Boolean = {
    trouverParMatricule(matricule).isDefined
  }

  def emailExiste(email: String): Boolean = withConnection { implicit conn =>
    SQL"SELECT COUNT(*) FROM etudiants WHERE email = $email".as(scalar[Long].single) > 0
  }
}
