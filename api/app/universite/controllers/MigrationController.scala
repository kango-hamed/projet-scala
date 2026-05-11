package universite.controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import universite.repositories._
import universite.models._
import scala.io.Source
import java.io.File
import scala.util.Try
import anorm._
import universite.actions.AdminAction

@Singleton
class MigrationController @Inject()(
  cc: ControllerComponents,
  adminAction: AdminAction,
  etudRepo: EtudiantRepository,
...
  // GET /api/migrate
  def migrate() = adminAction { request =>
    try {
      val results = scala.collection.mutable.Map[String, Int]()
      
      // 1. Filieres
      val filieres = chargerCSV("data/filieres.csv", Filiere.fromCSV)
      filieres.foreach(f => filiereRepo.db.withConnection { implicit c => 
        SQL"INSERT INTO filieres (id_filiere, nom_filiere, responsable) VALUES (${f.idFiliere}, ${f.nomFiliere}, ${f.responsable}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("filieres") = filieres.size

      // 2. Formations
      val formations = chargerCSV("data/formations.csv", Formation.fromCSV)
      formations.foreach(f => formRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO formations (id_formation, nom_formation, description, duree_annees, responsable) VALUES (${f.idFormation}, ${f.nomFormation}, ${f.description}, ${f.dureeAnnees}, ${f.responsable}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("formations") = formations.size

      // 3. Niveaux
      val niveaux = chargerCSV("data/niveaux.csv", Niveau.fromCSV)
      niveaux.foreach(n => formRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO niveaux (id_niveau, filiere, niveau_etudes, semestres) VALUES (${n.idNiveau}, ${n.filiere}, ${n.niveauEtudes.nom}, ${n.semestres.mkString(";")}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("niveaux") = niveaux.size

      // 4. Semestres
      val semestres = chargerCSV("data/semestres.csv", Semestre.fromCSV)
      semestres.foreach(s => formRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO semestres (id_semestre, nom_semestre, niveau, filiere, ues) VALUES (${s.idSemestre}, ${s.nomSemestre}, ${s.niveau}, ${s.filiere}, ${s.uniteEnseignements.mkString(";")}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("semestres") = semestres.size

      // 5. UEs
      val ues = chargerCSV("data/ues.csv", UniteEnseignement.fromCSV)
      ues.foreach(u => formRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO ues (id_ue, nom_ue, filiere, niveau, semestre, coefficient_total, matieres) VALUES (${u.idUE}, ${u.nomUE}, ${u.filiere}, ${u.niveau}, ${u.semestre}, ${u.coefficientTotal}, ${u.matieres.mkString(";")}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("ues") = ues.size

      // 6. Enseignants
      val enseignants = chargerCSV("data/enseignants.csv", Enseignant.fromCSV)
      enseignants.foreach(e => ensRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO enseignants (id_enseignant, nom, prenom, grade, specialite, departement, email, telephone) VALUES (${e.idEnseignant}, ${e.nom}, ${e.prenom}, ${e.grade}, ${e.specialite}, ${e.departement}, ${e.email}, ${e.telephone}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("enseignants") = enseignants.size

      // 7. Matieres
      val matieres = chargerCSV("data/matieres.csv", Matiere.fromCSV)
      matieres.foreach(m => matiereRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO matieres (id_matiere, nom_matiere, ue, coefficient, volume_horaire, id_enseignant) VALUES (${m.idMatiere}, ${m.nomMatiere}, ${m.ue}, ${m.coefficient}, ${m.volumeHoraire}, ${m.idEnseignant}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("matieres") = matieres.size

      // 8. Etudiants
      val etudiants = chargerCSV("data/etudiants.csv", Etudiant.fromCSV)
      etudiants.foreach(e => etudRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO etudiants (matricule, nom, prenom, sexe, date_naissance, email, telephone, filiere, niveau, annee, statut) VALUES (${e.matricule}, ${e.nom}, ${e.prenom}, ${e.sexe}, ${e.dateNaissance}, ${e.email}, ${e.telephone}, ${e.filiere}, ${e.niveau}, ${e.annee}, ${e.statut.toString.toLowerCase}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("etudiants") = etudiants.size

      // 9. Inscriptions
      val inscriptions = chargerCSV("data/inscriptions.csv", Inscription.fromCSV)
      inscriptions.foreach(i => inscrRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO inscriptions (id_inscription, matricule, filiere, niveau, annee, statut) VALUES (${i.idInscription}, ${i.matricule}, ${i.filiere}, ${i.niveau}, ${i.annee}, ${i.statut.toString.toLowerCase}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("inscriptions") = inscriptions.size

      // 10. Notes
      val notes = chargerCSV("data/notes.csv", Note.fromCSV)
      notes.foreach(n => noteRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO notes (id_note, matricule, id_matiere, controle_continu, examen) VALUES (${n.idNote}, ${n.matricule}, ${n.idMatiere}, ${n.controleContinue}, ${n.examen}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("notes") = notes.size

      // 11. Absences
      val absences = chargerCSV("data/absences.csv", Absence.fromCSV)
      absences.foreach(a => absRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO absences (id_absence, matricule, id_matiere, date_absence, heures, justifiee) VALUES (${a.idAbsence}, ${a.matricule}, ${a.idMatiere}, ${a.date}, ${a.heures}, ${a.justifiee}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("absences") = absences.size

      // 12. Paiements
      val paiements = chargerCSV("data/paiements.csv", Paiement.fromCSV)
      paiements.foreach(p => paiRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO paiements (id_paiement, matricule, montant_total, montant_paye, date_paiement, mode) VALUES (${p.idPaiement}, ${p.matricule}, ${p.montantTotal}, ${p.montantPaye}, ${p.datePaiement}, ${p.mode}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("paiements") = paiements.size

      // 13. Utilisateurs
      val utilisateurs = chargerCSV("data/utilisateurs.csv", Utilisateur.fromCSV)
      utilisateurs.foreach(u => userRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO utilisateurs (id_utilisateur, email, password_hash, role, id_profil, actif) VALUES (${u.idUtilisateur}, ${u.email}, ${u.motDePasseHash}, ${u.role.code}, ${u.idProfil}, ${u.actif}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("utilisateurs") = utilisateurs.size

      // 14. Salles
      val salles = chargerCSV("data/salles.csv", Salle.fromCSV)
      salles.foreach(s => salleRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO salles (id_salle, nom_salle, capacite, type_salle) VALUES (${s.idSalle}, ${s.nomSalle}, ${s.capacite}, ${s.typeSalle}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("salles") = salles.size

      // 15. Seances (Emploi du temps)
      val seances = chargerCSV("data/emplois_du_temps.csv", SeanceCours.fromCSV)
      seances.foreach(s => seanceRepo.db.withConnection { implicit c =>
        SQL"INSERT INTO seances_cours (id_seance, id_matiere, id_enseignant, id_salle, jour, heure_debut, heure_fin, filiere, niveau) VALUES (${s.idSeance}, ${s.idMatiere}, ${s.idEnseignant}, ${s.idSalle}, ${s.jour}, ${s.heureDebut}, ${s.heureFin}, ${s.filiere}, ${s.niveau}) ON CONFLICT DO NOTHING".executeUpdate()
      })
      results("seances") = seances.size

      Ok(Json.obj("success" -> true, "results" -> Json.toJson(results.toMap)))
    } catch {
      case e: Exception => 
        e.printStackTrace()
        InternalServerError(Json.obj("success" -> false, "error" -> e.getMessage))
    }
  }

  private def chargerCSV[A](chemin: String, parser: String => Option[A]): List[A] = {
    val file = new File(chemin)
    val actualFile = if (file.exists()) file else new File("../" + chemin)
    
    if (!actualFile.exists()) {
      println(s"Fichier non trouvé: ${actualFile.getAbsolutePath}")
      return List.empty
    }

    val source = Source.fromFile(actualFile)
    try {
      source.getLines().drop(1).filter(_.nonEmpty).flatMap(parser).toList
    } finally {
      source.close()
    }
  }
}
