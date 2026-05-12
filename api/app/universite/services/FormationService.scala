package universite.services

import universite.models._
import universite.repositories._

// ─────────────────────────────────────────────
// Service : FormationService
// Navigation dans la hiérarchie des formations
// ─────────────────────────────────────────────
class FormationService @javax.inject.Inject()(
  val formationRepo: FormationRepository,
  val matiereRepo: MatiereRepository
) {

  // ─── Navigation hiérarchique ──────────────
  
  def obtenirArbreFormation(filiere: String): List[(Niveau, List[(Semestre, List[UniteEnseignement])])] = {
    val niveaux = formationRepo.niveauxParFiliere(filiere)
    niveaux.map { niveau =>
      val semestres = formationRepo.semestresParNiveau(niveau.idNiveau)
      val semestresAvecUEs = semestres.map { semestre =>
        val ues = formationRepo.uesParSemestre(semestre.idSemestre)
        (semestre, ues)
      }
      (niveau, semestresAvecUEs)
    }
  }
  
  def obtenirStructureComplete(): Map[String, List[(Niveau, List[(Semestre, List[UniteEnseignement])])]] = {
    val toutesFilieres = formationRepo.toutesLesFormations().map(_.idFormation)
    toutesFilieres.map(fil => fil -> obtenirArbreFormation(fil)).toMap
  }
  
  // ─── Navigation par niveau ────────────────
  
  def matieresParNiveau(filiere: String, niveau: String): List[Matiere] = {
    val matieres = matiereRepo.toutesLesMatieres()
    matieres.filter(m => 
      m.idMatiere.contains(niveau) || 
      formationRepo.uesParSemestre(niveau).exists(_.matieres.contains(m.idMatiere))
    )
  }
  
  def matieresParUE(idUE: String): List[Matiere] = {
    val toutesMatieres = matiereRepo.toutesLesMatieres()
    toutesMatieres.filter(m => m.ue == idUE)
  }
  
  // ─── Statistiques ─────────────────────────
  
  def coefficientTotalParUE(idUE: String): Int = {
    formationRepo.trouverUEParId(idUE)
      .map(_.coefficientTotal)
      .getOrElse(0)
  }
  
  def matiereLaPlusChargee(filiere: String): Option[Matiere] = {
    val matieres = matiereRepo.toutesLesMatieres()
      .filter(_.idMatiere.contains(filiere) || filiere == "ALL")
    if (matieres.isEmpty) None
    else Some(matieres.maxBy(_.volumeHoraire))
  }
  
  def volumesHorairesParNiveau(filiere: String): Map[String, Int] = {
    val arbre = obtenirArbreFormation(filiere)
    arbre.map { case (niveau, semestres) =>
      val totalVolume = semestres.flatMap { case (_, ues) =>
        ues.flatMap { ue =>
          matieresParUE(ue.idUE).map(_.volumeHoraire)
        }
      }.sum
      niveau.nomComplet -> totalVolume
    }.toMap
  }
  
  // ─── Affichage console ────────────────────
  
  def afficherArbreFormation(filiere: String): Unit = {
    val arbre = obtenirArbreFormation(filiere)
    println(s"\n═══ HIÉRARCHIE : $filiere ═══")
    arbre.foreach { case (niveau, semestres) =>
      println(s"\n📚 ${niveau.nomComplet}")
      semestres.foreach { case (semestre, ues) =>
        println(s"   📅 ${semestre.nomSemestre}")
        ues.foreach { ue =>
          println(s"      📖 ${ue.nomUE} (Coeff: ${ue.coefficientTotal})")
          val matieres = matieresParUE(ue.idUE)
          matieres.foreach { m =>
            println(s"         └─ ${m.nomMatiere} (${m.volumeHoraire}h)")
          }
        }
      }
    }
  }
  
  def afficherToutesFormations(): Unit = {
    val formations = formationRepo.toutesLesFormations()
    println(s"\n═══ FORMATIONS (${formations.size}) ═══")
    formations.foreach(_.afficher())
  }
  
  def afficherNiveauxParFiliere(filiere: String): Unit = {
    val niveaux = formationRepo.niveauxParFiliere(filiere)
    println(s"\n═══ NIVEAUX : $filiere ═══")
    niveaux.foreach(_.afficher())
  }
}
