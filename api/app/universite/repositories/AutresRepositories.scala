package universite.repositories

import universite.models._
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : Enseignant
// ─────────────────────────────────────────────
@Singleton
class EnseignantRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_enseignant") ~
    get[String]("nom") ~
    get[String]("prenom") ~
    get[String]("grade") ~
    get[String]("specialite") ~
    get[String]("departement") ~
    get[String]("email") ~
    get[String]("telephone") map {
      case id ~ nom ~ pre ~ grade ~ spec ~ dept ~ email ~ tel =>
        Enseignant(id, nom, pre, grade, spec, dept, email, tel)
    }
  }

  def tousLesEnseignants(): List[Enseignant] = withConnection { implicit conn =>
    SQL"SELECT * FROM enseignants".as(parser.*)
  }

  def trouverParId(id: String): Option[Enseignant] = withConnection { implicit conn =>
    SQL"SELECT * FROM enseignants WHERE id_enseignant = $id".as(parser.singleOpt)
  }

  def parDepartement(dept: String): List[Enseignant] = withConnection { implicit conn =>
    SQL"SELECT * FROM enseignants WHERE departement = $dept".as(parser.*)
  }

  def departementsUniques(): Set[String] = withConnection { implicit conn =>
    SQL"SELECT DISTINCT departement FROM enseignants".as(scalar[String].*).toSet
  }
}

// ─────────────────────────────────────────────
// Repository : Matiere
// ─────────────────────────────────────────────
@Singleton
class MatiereRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_matiere") ~
    get[String]("nom_matiere") ~
    get[String]("ue") ~
    get[Int]("coefficient") ~
    get[Int]("volume_horaire") ~
    get[String]("id_enseignant") map {
      case id ~ nom ~ ue ~ coeff ~ vol ~ ens =>
        Matiere(id, nom, ue, coeff, vol, ens)
    }
  }

  def toutesLesMatieres(): List[Matiere] = withConnection { implicit conn =>
    SQL"SELECT * FROM matieres".as(parser.*)
  }

  def trouverParId(id: String): Option[Matiere] = withConnection { implicit conn =>
    SQL"SELECT * FROM matieres WHERE id_matiere = $id".as(parser.singleOpt)
  }

  def parEnseignant(idEnseignant: String): List[Matiere] = withConnection { implicit conn =>
    SQL"SELECT * FROM matieres WHERE id_enseignant = $idEnseignant".as(parser.*)
  }

  // Map idMatiere -> Matiere (accès O(1) depuis les services)
  def indexParId(): Map[String, Matiere] = {
    toutesLesMatieres().map(m => m.idMatiere -> m).toMap
  }

  // Volume horaire total d'un enseignant
  def volumeHoraireEnseignant(idEnseignant: String): Int = withConnection { implicit conn =>
    SQL"SELECT SUM(volume_horaire) FROM matieres WHERE id_enseignant = $idEnseignant".as(scalar[Option[Int]].single).getOrElse(0)
  }
}

// ─────────────────────────────────────────────
// Repository : Inscription
// ─────────────────────────────────────────────
@Singleton
class InscriptionRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_inscription") ~
    get[String]("matricule") ~
    get[String]("filiere") ~
    get[String]("niveau") ~
    get[String]("annee") ~
    get[String]("statut") map {
      case id ~ mat ~ fil ~ niv ~ an ~ statut =>
        Inscription(id, mat, fil, niv, an, StatutInscription.fromString(statut))
    }
  }

  def toutesLesInscriptions(): List[Inscription] = withConnection { implicit conn =>
    SQL"SELECT * FROM inscriptions".as(parser.*)
  }

  def inscriptionsParEtudiant(matricule: String): List[Inscription] = withConnection { implicit conn =>
    SQL"SELECT * FROM inscriptions WHERE matricule = $matricule".as(parser.*)
  }

  def inscriptionsValidees(): List[Inscription] = withConnection { implicit conn =>
    SQL"SELECT * FROM inscriptions WHERE statut = 'validee'".as(parser.*)
  }

  def inscriptionsEnAttente(): List[Inscription] = withConnection { implicit conn =>
    SQL"SELECT * FROM inscriptions WHERE statut = 'en attente'".as(parser.*)
  }

  // Vérifier double inscription : un étudiant ne s'inscrit pas deux fois la même année
  def estDejaInscrit(matricule: String, annee: String): Boolean = withConnection { implicit conn =>
    SQL"SELECT COUNT(*) FROM inscriptions WHERE matricule = $matricule AND annee = $annee".as(scalar[Long].single) > 0
  }
}

// ─────────────────────────────────────────────
// Repository : SeanceCours (emplois du temps)
// ─────────────────────────────────────────────
@Singleton
class SeanceCoursRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_seance") ~
    get[String]("id_matiere") ~
    get[String]("id_enseignant") ~
    get[String]("id_salle") ~
    get[String]("jour") ~
    get[String]("heure_debut") ~
    get[String]("heure_fin") ~
    get[String]("filiere") ~
    get[String]("niveau") map {
      case id ~ mat ~ ens ~ sal ~ jr ~ hd ~ hf ~ fil ~ niv =>
        SeanceCours(id, mat, ens, sal, jr, hd, hf, fil, niv)
    }
  }

  def toutesLesSeances(): List[SeanceCours] = withConnection { implicit conn =>
    SQL"SELECT * FROM seances_cours".as(parser.*)
  }

  def parFiliere(filiere: String): List[SeanceCours] = withConnection { implicit conn =>
    SQL"SELECT * FROM seances_cours WHERE filiere = $filiere".as(parser.*)
  }

  def parEnseignant(idEnseignant: String): List[SeanceCours] = withConnection { implicit conn =>
    SQL"SELECT * FROM seances_cours WHERE id_enseignant = $idEnseignant".as(parser.*)
  }

  def parSalle(idSalle: String): List[SeanceCours] = withConnection { implicit conn =>
    SQL"SELECT * FROM seances_cours WHERE id_salle = $idSalle".as(parser.*)
  }

  // Détection des conflits de salle
  def conflitsDeSalle(): List[(SeanceCours, SeanceCours)] = {
    val seances = toutesLesSeances()
    for {
      a <- seances
      b <- seances
      if a.idSeance < b.idSeance && a.enConflit(b)
    } yield (a, b)
  }
}

// ─────────────────────────────────────────────
// Repository : Filiere
// ─────────────────────────────────────────────
@Singleton
class FiliereRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_filiere") ~
    get[String]("nom_filiere") ~
    get[String]("responsable") map {
      case id ~ nom ~ resp =>
        Filiere(id, nom, resp)
    }
  }

  def toutesLesFilieres(): List[Filiere] = withConnection { implicit conn =>
    SQL"SELECT * FROM filieres".as(parser.*)
  }

  def trouverParNom(nom: String): Option[Filiere] = withConnection { implicit conn =>
    SQL"SELECT * FROM filieres WHERE nom_filiere = $nom".as(parser.singleOpt)
  }
}

// ─────────────────────────────────────────────
// Repository : Salle
// ─────────────────────────────────────────────
@Singleton
class SalleRepository @Inject()(val db: Database) extends BaseRepository {

  private val parser = {
    get[String]("id_salle") ~
    get[String]("nom_salle") ~
    get[Int]("capacite") ~
    get[String]("type_salle") map {
      case id ~ nom ~ cap ~ typ =>
        Salle(id, nom, cap, typ)
    }
  }

  def toutesLesSalles(): List[Salle] = withConnection { implicit conn =>
    SQL"SELECT * FROM salles".as(parser.*)
  }

  def parType(typeSalle: String): List[Salle] = withConnection { implicit conn =>
    SQL"SELECT * FROM salles WHERE type_salle = $typeSalle".as(parser.*)
  }
}
