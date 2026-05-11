package universite.models

import universite.traits._
import scala.util.Try

// ─────────────────────────────────────────────
// Case class : Absence
// ─────────────────────────────────────────────
case class Absence(
  idAbsence  : String,
  matricule  : String,
  idMatiere  : String,
  date       : String,
  heures     : Int,
  justifiee  : Boolean
) extends Identifiable with Affichable {

  override def id: String = idAbsence

  override def afficher(): Unit = {
    val justif = if (justifiee) "Justifiée" else "Non justifiée"
    println(s"  [$idAbsence] $matricule  |  $idMatiere  |  $date  |  ${heures}h  |  $justif")
  }
}

object Absence {
  // id_absence,matricule,matiere,date_absence,heures,justifiee
  def fromCSV(ligne: String): Option[Absence] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 6) None
    else for {
      h <- Try(cols(4).toInt).toOption
    } yield Absence(
      idAbsence = cols(0),
      matricule = cols(1),
      idMatiere = cols(2),
      date      = cols(3),
      heures    = h,
      justifiee = cols(5).equalsIgnoreCase("Oui")
    )
  }
}
