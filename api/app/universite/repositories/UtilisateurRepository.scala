package universite.repositories

import universite.models._
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : UtilisateurRepository
// Gestion des comptes utilisateurs avec PostgreSQL et Anorm
// ─────────────────────────────────────────────
@Singleton
class UtilisateurRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_utilisateur") ~
    get[String]("email") ~
    get[String]("password_hash") ~
    get[String]("role") ~
    get[String]("id_profil") ~
    get[Boolean]("actif") map {
      case id ~ email ~ pwd ~ role ~ idProfil ~ actif =>
        Utilisateur(id, email, pwd, RoleUtilisateur.fromString(role), idProfil, actif)
    }
  }

  def tousLesUtilisateurs(): List[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs".as(parser.*)
  }

  def trouverParId(id: String): Option[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs WHERE id_utilisateur = $id".as(parser.singleOpt)
  }

  def trouverParEmail(email: String): Option[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs WHERE email = $email".as(parser.singleOpt)
  }

  def trouverParProfil(idProfil: String): Option[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs WHERE id_profil = $idProfil".as(parser.singleOpt)
  }

  def utilisateursParRole(role: RoleUtilisateur): List[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs WHERE role = ${role.code}".as(parser.*)
  }

  def utilisateursActifs(): List[Utilisateur] = withConnection { implicit conn =>
    SQL"SELECT * FROM utilisateurs WHERE actif = true".as(parser.*)
  }

  def emailExiste(email: String): Boolean = {
    trouverParEmail(email).isDefined
  }

  def sauvegarder(utilisateur: Utilisateur): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO utilisateurs (id_utilisateur, email, password_hash, role, id_profil, actif)
      VALUES (${utilisateur.idUtilisateur}, ${utilisateur.email}, ${utilisateur.motDePasseHash}, ${utilisateur.role.code}, ${utilisateur.idProfil}, ${utilisateur.actif})
      ON CONFLICT (id_utilisateur) DO UPDATE SET
      email = EXCLUDED.email,
      password_hash = EXCLUDED.password_hash,
      role = EXCLUDED.role,
      id_profil = EXCLUDED.id_profil,
      actif = EXCLUDED.actif
    """.executeUpdate() > 0
  }

  def changerStatut(id: String, actif: Boolean): Boolean = withConnection { implicit conn =>
    SQL"UPDATE utilisateurs SET actif = $actif WHERE id_utilisateur = $id".executeUpdate() > 0
  }
}
