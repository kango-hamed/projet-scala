package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Case class : Enseignant
// ─────────────────────────────────────────────
case class Enseignant(
  idEnseignant : String,
  nom          : String,
  prenom       : String,
  grade        : String,
  specialite   : String,
  departement  : String,
  email        : String,
  telephone    : String
) extends Identifiable with Affichable {

  override def id: String = idEnseignant

  override def afficher(): Unit = {
    println(s"┌─────────────────────────────────────")
    println(s"│ ID        : $idEnseignant")
    println(s"│ Nom       : $prenom $nom")
    println(s"│ Grade     : $grade")
    println(s"│ Spécialité: $specialite")
    println(s"│ Départ.   : $departement")
    println(s"└─────────────────────────────────────")
  }

  def nomComplet: String = s"$prenom $nom"
}

object Enseignant {
  // id_enseignant,nom,prenom,grade,specialite,departement,email,telephone
  def fromCSV(ligne: String): Option[Enseignant] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 8) None
    else Some(Enseignant(
      idEnseignant = cols(0),
      nom          = cols(1),
      prenom       = cols(2),
      grade        = cols(3),
      specialite   = cols(4),
      departement  = cols(5),
      email        = cols(6),
      telephone    = cols(7)
    ))
  }
}
