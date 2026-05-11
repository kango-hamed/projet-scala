package universite.models

import universite.traits._
import scala.util.Try

// ─────────────────────────────────────────────
// Enum-like : Décision académique
// ─────────────────────────────────────────────
sealed trait DecisionAcademique
case object Admis        extends DecisionAcademique
case object Ajourne      extends DecisionAcademique
case object Redoublement extends DecisionAcademique

object DecisionAcademique {
  def fromMoyenne(moyenne: Double): DecisionAcademique =
    if (moyenne >= 10.0) Admis
    else if (moyenne >= 7.0) Ajourne
    else Redoublement
}

// ─────────────────────────────────────────────
// Case class : Note
// Validable → vérifie que les notes sont dans [0, 20]
// Calculable → peut calculer la moyenne matière
// ─────────────────────────────────────────────
case class Note(
  idNote          : String,
  matricule       : String,
  idMatiere       : String,
  controleContinue: Double,
  examen          : Double
) extends Identifiable with Validable with Calculable {

  override def id: String = idNote

  // Formule : 40% CC + 60% Examen
  def moyenneMatiere: Double =
    (controleContinue * 0.4) + (examen * 0.6)

  def decision: DecisionAcademique =
    DecisionAcademique.fromMoyenne(moyenneMatiere)

  // Validable
  override def estValide: Boolean = erreurs.isEmpty

  override def erreurs: List[String] = List(
    if (controleContinue < 0 || controleContinue > 20) Some(s"Note CC invalide ($controleContinue) pour $idNote") else None,
    if (examen           < 0 || examen           > 20) Some(s"Note examen invalide ($examen) pour $idNote")       else None
  ).flatten

  def afficher(): Unit =
    println(f"  [$idNote] Étudiant: $matricule  |  Matière: $idMatiere  |  CC: $controleContinue%.1f  |  Exam: $examen%.1f  |  Moy: ${moyenneMatiere}%.2f  |  Décision: $decision")
}

object Note {
  // id_note,matricule,matiere,controle_continu,examen
  def fromCSV(ligne: String): Option[Note] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 5) None
    else for {
      cc    <- Try(cols(3).toDouble).toOption
      exam  <- Try(cols(4).toDouble).toOption
    } yield Note(cols(0), cols(1), cols(2), cc, exam)
  }
}
