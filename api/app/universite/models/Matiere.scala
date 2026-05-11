package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Case class : Matiere
// ─────────────────────────────────────────────
case class Matiere(
  idMatiere    : String,
  nomMatiere   : String,
  ue           : String,
  coefficient  : Int,
  volumeHoraire: Int,
  idEnseignant : String
) extends Identifiable with Affichable {

  override def id: String = idMatiere

  override def afficher(): Unit = {
    println(s"  [$idMatiere] $nomMatiere  |  UE: $ue  |  Coeff: $coefficient  |  ${volumeHoraire}h")
  }
}

object Matiere {
  // id_matiere,nom_matiere,ue,coefficient,volume_horaire,enseignant
  def fromCSV(ligne: String): Option[Matiere] = {
    val cols = ligne.split(",").map(_.trim)
    import scala.util.Try
    if (cols.length < 6) None
    else for {
      coeff  <- Try(cols(3).toInt).toOption
      volume <- Try(cols(4).toInt).toOption
    } yield Matiere(cols(0), cols(1), cols(2), coeff, volume, cols(5))
  }
}

// ─────────────────────────────────────────────
// Case class : Filiere
// ─────────────────────────────────────────────
case class Filiere(
  idFiliere    : String,
  nomFiliere   : String,
  responsable  : String
) extends Identifiable with Affichable {

  override def id: String = idFiliere

  override def afficher(): Unit =
    println(s"  [$idFiliere] $nomFiliere  (Responsable: $responsable)")
}

object Filiere {
  def fromCSV(ligne: String): Option[Filiere] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 3) None
    else Some(Filiere(cols(0), cols(1), cols(2)))
  }
}

// ─────────────────────────────────────────────
// Case class : Salle
// ─────────────────────────────────────────────
case class Salle(
  idSalle  : String,
  nomSalle : String,
  capacite : Int,
  typeSalle: String
) extends Identifiable with Affichable {

  override def id: String = idSalle

  override def afficher(): Unit =
    println(s"  [$idSalle] $nomSalle  |  Capacité: $capacite  |  Type: $typeSalle")
}

object Salle {
  def fromCSV(ligne: String): Option[Salle] = {
    val cols = ligne.split(",").map(_.trim)
    import scala.util.Try
    if (cols.length < 4) None
    else Try(cols(2).toInt).toOption.map(cap => Salle(cols(0), cols(1), cap, cols(3)))
  }
}
