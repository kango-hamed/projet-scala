package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Case class : Formation
// Représente la structure complète : Filière > Niveau > Semestre > UE > Matière
// ─────────────────────────────────────────────
case class Formation(
  idFormation: String,
  nomFormation: String,
  description: String,
  dureeAnnees: Int,
  responsable: String
) extends Identifiable with Affichable {
  
  override def id: String = idFormation
  
  override def afficher(): Unit = {
    println(s"┌─────────────────────────────────────")
    println(s"│ Formation: $nomFormation")
    println(s"│ ID: $idFormation")
    println(s"│ Durée: ${dureeAnnees} années")
    println(s"│ Responsable: $responsable")
    println(s"└─────────────────────────────────────")
  }
}

object Formation {
  // id_formation,nom_formation,description,duree,responsable
  def fromCSV(ligne: String): Option[Formation] = {
    val cols = ligne.split(",").map(_.trim)
    import scala.util.Try
    if (cols.length < 5) None
    else for {
      duree <- Try(cols(3).toInt).toOption
    } yield Formation(
      idFormation = cols(0),
      nomFormation = cols(1),
      description = cols(2),
      dureeAnnees = duree,
      responsable = cols(4)
    )
  }
}
