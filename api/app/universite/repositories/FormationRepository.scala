package universite.repositories

import universite.models._
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import anorm._
import anorm.SqlParser._

// ─────────────────────────────────────────────
// Repository : FormationRepository
// Accès aux données des formations, niveaux, semestres et UEs via PostgreSQL
// ─────────────────────────────────────────────
@Singleton
class FormationRepository @Inject()(val db: Database) extends BaseRepository {

  // ─── Parsers ──────────────────────────────

  private val formationParser = {
    get[String]("id_formation") ~
    get[String]("nom_formation") ~
    get[String]("description") ~
    get[Int]("duree_annees") ~
    get[String]("responsable") map {
      case id ~ nom ~ desc ~ duree ~ resp =>
        Formation(id, nom, desc, duree, resp)
    }
  }

  private val niveauParser = {
    get[String]("id_niveau") ~
    get[String]("filiere") ~
    get[String]("niveau_etudes") ~
    get[Option[String]]("semestres") map {
      case id ~ fil ~ niv ~ sems =>
        Niveau(id, fil, NiveauEtudes.fromString(niv), sems.map(_.split(";").toList).getOrElse(List.empty))
    }
  }

  private val semestreParser = {
    get[String]("id_semestre") ~
    get[String]("nom_semestre") ~
    get[String]("niveau") ~
    get[String]("filiere") ~
    get[Option[String]]("ues") map {
      case id ~ nom ~ niv ~ fil ~ ues =>
        Semestre(id, nom, niv, fil, ues.map(_.split(";").toList).getOrElse(List.empty))
    }
  }

  private val ueParser = {
    get[String]("id_ue") ~
    get[String]("nom_ue") ~
    get[String]("filiere") ~
    get[String]("niveau") ~
    get[String]("semestre") ~
    get[Int]("coefficient_total") ~
    get[Option[String]]("matieres") map {
      case id ~ nom ~ fil ~ niv ~ sem ~ coeff ~ mats =>
        UniteEnseignement(id, nom, fil, niv, sem, coeff, mats.map(_.split(";").toList).getOrElse(List.empty))
    }
  }

  // ─── Formations ───────────────────────────
  
  def toutesLesFormations(): List[Formation] = withConnection { implicit conn =>
    SQL"SELECT * FROM formations".as(formationParser.*)
  }
  
  def trouverFormationParId(id: String): Option[Formation] = withConnection { implicit conn =>
    SQL"SELECT * FROM formations WHERE id_formation = $id".as(formationParser.singleOpt)
  }
  
  // ─── Niveaux ──────────────────────────────
  
  def tousLesNiveaux(): List[Niveau] = withConnection { implicit conn =>
    SQL"SELECT * FROM niveaux".as(niveauParser.*)
  }
  
  def niveauxParFiliere(filiere: String): List[Niveau] = withConnection { implicit conn =>
    SQL"SELECT * FROM niveaux WHERE filiere = $filiere".as(niveauParser.*)
  }
  
  def trouverNiveauParId(id: String): Option[Niveau] = withConnection { implicit conn =>
    SQL"SELECT * FROM niveaux WHERE id_niveau = $id".as(niveauParser.singleOpt)
  }
  
  // ─── Semestres ────────────────────────────
  
  def tousLesSemestres(): List[Semestre] = withConnection { implicit conn =>
    SQL"SELECT * FROM semestres".as(semestreParser.*)
  }
  
  def semestresParNiveau(niveau: String): List[Semestre] = withConnection { implicit conn =>
    SQL"SELECT * FROM semestres WHERE niveau = $niveau".as(semestreParser.*)
  }
  
  def trouverSemestreParId(id: String): Option[Semestre] = withConnection { implicit conn =>
    SQL"SELECT * FROM semestres WHERE id_semestre = $id".as(semestreParser.singleOpt)
  }
  
  // ─── Unités d'Enseignement ────────────────
  
  def toutesLesUEs(): List[UniteEnseignement] = withConnection { implicit conn =>
    SQL"SELECT * FROM ues".as(ueParser.*)
  }
  
  def uesParSemestre(semestre: String): List[UniteEnseignement] = withConnection { implicit conn =>
    SQL"SELECT * FROM ues WHERE semestre = $semestre".as(ueParser.*)
  }
  
  def uesParFiliere(filiere: String): List[UniteEnseignement] = withConnection { implicit conn =>
    SQL"SELECT * FROM ues WHERE filiere = $filiere".as(ueParser.*)
  }
  
  def trouverUEParId(id: String): Option[UniteEnseignement] = withConnection { implicit conn =>
    SQL"SELECT * FROM ues WHERE id_ue = $id".as(ueParser.singleOpt)
  }

  def trouverParId(id: String): Option[Formation] = trouverFormationParId(id)

  // ─── CRUD Operations ────────────────────────

  // --- Formation CRUD ---
  def creerFormation(formation: Formation): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO formations (id_formation, nom_formation, description, duree_annees, responsable)
      VALUES (${formation.idFormation}, ${formation.nomFormation}, ${formation.description}, ${formation.dureeAnnees}, ${formation.responsable})
    """.executeUpdate() > 0
  }

  def mettreAJourFormation(id: String, formation: Formation): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE formations SET
        nom_formation = ${formation.nomFormation},
        description = ${formation.description},
        duree_annees = ${formation.dureeAnnees},
        responsable = ${formation.responsable}
      WHERE id_formation = $id
    """.executeUpdate() > 0
  }

  def supprimerFormation(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM formations WHERE id_formation = $id".executeUpdate() > 0
  }

  def idFormationExiste(id: String): Boolean = {
    trouverFormationParId(id).isDefined
  }

  // --- Niveau CRUD ---
  def creerNiveau(niveau: Niveau): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO niveaux (id_niveau, filiere, niveau_etudes, semestres)
      VALUES (${niveau.idNiveau}, ${niveau.filiere}, ${niveau.niveauEtudes.toString}, ${niveau.semestres.mkString(";")})
    """.executeUpdate() > 0
  }

  def mettreAJourNiveau(id: String, niveau: Niveau): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE niveaux SET
        filiere = ${niveau.filiere},
        niveau_etudes = ${niveau.niveauEtudes.toString},
        semestres = ${niveau.semestres.mkString(";")}
      WHERE id_niveau = $id
    """.executeUpdate() > 0
  }

  def supprimerNiveau(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM niveaux WHERE id_niveau = $id".executeUpdate() > 0
  }

  def idNiveauExiste(id: String): Boolean = {
    trouverNiveauParId(id).isDefined
  }

  // --- Semestre CRUD ---
  def creerSemestre(semestre: Semestre): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO semestres (id_semestre, nom_semestre, niveau, filiere, ues)
      VALUES (${semestre.idSemestre}, ${semestre.nomSemestre}, ${semestre.niveau}, ${semestre.filiere}, ${semestre.uniteEnseignements.mkString(";")})
    """.executeUpdate() > 0
  }

  def mettreAJourSemestre(id: String, semestre: Semestre): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE semestres SET
        nom_semestre = ${semestre.nomSemestre},
        niveau = ${semestre.niveau},
        filiere = ${semestre.filiere},
        ues = ${semestre.uniteEnseignements.mkString(";")}
      WHERE id_semestre = $id
    """.executeUpdate() > 0
  }

  def supprimerSemestre(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM semestres WHERE id_semestre = $id".executeUpdate() > 0
  }

  def idSemestreExiste(id: String): Boolean = {
    trouverSemestreParId(id).isDefined
  }

  // --- UE CRUD ---
  def creerUE(ue: UniteEnseignement): Boolean = withConnection { implicit conn =>
    SQL"""
      INSERT INTO ues (id_ue, nom_ue, filiere, niveau, semestre, coefficient_total, matieres)
      VALUES (${ue.idUE}, ${ue.nomUE}, ${ue.filiere}, ${ue.niveau}, ${ue.semestre}, ${ue.coefficientTotal}, ${ue.matieres.mkString(";")})
    """.executeUpdate() > 0
  }

  def mettreAJourUE(id: String, ue: UniteEnseignement): Boolean = withConnection { implicit conn =>
    SQL"""
      UPDATE ues SET
        nom_ue = ${ue.nomUE},
        filiere = ${ue.filiere},
        niveau = ${ue.niveau},
        semestre = ${ue.semestre},
        coefficient_total = ${ue.coefficientTotal},
        matieres = ${ue.matieres.mkString(";")}
      WHERE id_ue = $id
    """.executeUpdate() > 0
  }

  def supprimerUE(id: String): Boolean = withConnection { implicit conn =>
    SQL"DELETE FROM ues WHERE id_ue = $id".executeUpdate() > 0
  }

  def idUEExiste(id: String): Boolean = {
    trouverUEParId(id).isDefined
  }
}
