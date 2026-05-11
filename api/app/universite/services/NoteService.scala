package universite.services

import universite.models._
import universite.repositories.{NoteRepository, EtudiantRepository, MatiereRepository}
import universite.traits._

// ─────────────────────────────────────────────
// Service : NoteService
// Logique métier sur les notes et moyennes
// ─────────────────────────────────────────────
class NoteService @javax.inject.Inject()(
  val noteRepo    : NoteRepository,
  val etudRepo    : EtudiantRepository,
  val matiereRepo : MatiereRepository
) extends Calculable {

  // ── Moyennes ─────────────────────────────

  def noteService_notesParEtudiant(matricule: String): List[Note] =
    noteRepo.notesParEtudiant(matricule)

  def notesInvalides(): List[Note] =
    noteRepo.notesInvalides()

  // Moyenne d'une note matière (40% CC + 60% exam)
  def moyenneMatiere(note: Note): Double =
    note.moyenneMatiere

  // Moyenne générale d'un étudiant (sur toutes ses matières)
  def moyenneGenerale(matricule: String): Double = {
    val notes = noteRepo.notesParEtudiant(matricule).filter(_.estValide)
    if (notes.isEmpty) 0.0
    else {
      val moyennes = notes.map(_.moyenneMatiere)
      // Utilise la fonction héritée de Calculable
      calculerMoyenne(moyennes)
    }
  }

  // Moyenne générale pondérée (tient compte des coefficients)
  def moyenneGeneralePonderee(matricule: String): Double = {
    val notes    = noteRepo.notesParEtudiant(matricule).filter(_.estValide)
    val matieres = matiereRepo.indexParId()
    if (notes.isEmpty) return 0.0

    val (sommePonderee, sommeCoeffs) = notes.foldLeft((0.0, 0)) {
      case ((accNote, accCoeff), note) =>
        val coeff = matieres.get(note.idMatiere).map(_.coefficient).getOrElse(1)
        (accNote + note.moyenneMatiere * coeff, accCoeff + coeff)
    }
    if (sommeCoeffs == 0) 0.0 else sommePonderee / sommeCoeffs
  }

  // Moyenne d'une matière sur tous les étudiants
  def moyenneParMatiere(idMatiere: String): Double = {
    val notes = noteRepo.notesParMatiere(idMatiere).filter(_.estValide)
    if (notes.isEmpty) 0.0
    else calculerMoyenne(notes.map(_.moyenneMatiere))
  }

  // Somme récursive des moyennes
  def sommeMoyennesRecursive(notes: List[Note]): Double = notes match {
    case Nil         => 0.0
    case head :: tail => head.moyenneMatiere + sommeMoyennesRecursive(tail)
  }

  // ── Décisions ────────────────────────────

  // Décision pour un étudiant (Admis / Ajourné / Redoublement)
  def decisionEtudiant(matricule: String): DecisionAcademique =
    DecisionAcademique.fromMoyenne(moyenneGenerale(matricule))

  // Pattern matching sur la décision pour affichage
  def afficherDecision(matricule: String): Unit = {
    val moy = moyenneGenerale(matricule)
    val dec = DecisionAcademique.fromMoyenne(moy)
    val msg = dec match {
      case Admis        => s"✓ ADMIS        (${f"$moy%.2f"}/20)"
      case Ajourne      => s"~ AJOURNÉ      (${f"$moy%.2f"}/20)"
      case Redoublement => s"✗ REDOUBLEMENT (${f"$moy%.2f"}/20)"
    }
    println(s"  $matricule  →  $msg")
  }

  // ── Classement ───────────────────────────

  // Classement des étudiants par moyenne décroissante
  def classementEtudiants(): List[(String, Double)] = {
    val etudiants = etudRepo.etudiantsActifs()
    etudiants
      .map(e => (e.matricule, moyenneGenerale(e.matricule)))
      .sortBy(-_._2)
  }

  // Top N étudiants
  def topEtudiants(n: Int = 5): List[(String, Double)] =
    classementEtudiants().take(n)

  // Étudiants ajournés
  def etudiantsAjournes(): List[String] =
    classementEtudiants()
      .filter { case (_, moy) => moy < 10.0 }
      .map(_._1)

  // Matières avec les moyennes les plus faibles
  def matieresDifficiles(): List[(String, Double)] = {
    val matieres = matiereRepo.toutesLesMatieres()
    matieres
      .map(m => (m.nomMatiere, moyenneParMatiere(m.idMatiere)))
      .filter(_._2 > 0)
      .sortBy(_._2)
  }

  // ── Relevé de notes ──────────────────────

  def releveDeNotes(matricule: String): Unit = {
    val notes    = noteRepo.notesParEtudiant(matricule).filter(_.estValide)
    val matieres = matiereRepo.indexParId()

    println(s"\n╔═══════════════════════════════════════════╗")
    println(s"║         RELEVÉ DE NOTES : $matricule         ║")
    println(s"╠═══════════════════════════════════════════╣")
    println(f"  ${"Matière"}%-30s ${"CC"}%5s ${"Exam"}%5s ${"Moy"}%6s")
    println(s"  " + "─" * 50)

    notes.foreach { note =>
      val nomMat = matieres.get(note.idMatiere).map(_.nomMatiere).getOrElse(note.idMatiere)
      println(f"  ${nomMat}%-30s ${note.controleContinue}%5.1f ${note.examen}%5.1f ${note.moyenneMatiere}%6.2f")
    }

    println(s"  " + "─" * 50)
    println(f"  ${"Moyenne générale"}%-30s ${" "}%5s ${" "}%5s ${moyenneGenerale(matricule)}%6.2f")
    println(s"  Décision : ${decisionEtudiant(matricule)}")
    println(s"╚═══════════════════════════════════════════╝")
  }

  // ── Affichage console ────────────────────

  def afficherClassement(): Unit = {
    val classement = classementEtudiants()
    println("\n═══ CLASSEMENT DES ÉTUDIANTS ═══")
    classement.zipWithIndex.foreach { case ((mat, moy), idx) =>
      println(f"  ${idx + 1}%2d. $mat  →  $moy%.2f/20")
    }
  }

  def afficherNotesInvalides(): Unit = {
    val invalides = noteRepo.notesInvalides()
    if (invalides.isEmpty) println("\n✓ Aucune note invalide détectée.")
    else {
      println(s"\n⚠  NOTES INVALIDES (${invalides.size})")
      invalides.foreach { n =>
        n.erreurs.foreach(e => println(s"  $e"))
      }
    }
  }

  def afficherMatieresDifficiles(): Unit = {
    println("\n═══ MATIÈRES PAR DIFFICULTÉ (moyennes croissantes) ═══")
    matieresDifficiles().foreach { case (nom, moy) =>
      println(f"  $nom%-35s →  $moy%.2f/20")
    }
  }
}
