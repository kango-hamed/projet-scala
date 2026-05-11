package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Enum-like : Statut d'un étudiant
// Utilise le pattern matching dans les services
// ─────────────────────────────────────────────
sealed trait StatutEtudiant
case object Actif    extends StatutEtudiant
case object Suspendu extends StatutEtudiant
case object Diplome  extends StatutEtudiant
case object Inconnu  extends StatutEtudiant

object StatutEtudiant {
  def fromString(s: String): StatutEtudiant = s.trim.toLowerCase match {
    case "actif"     => Actif
    case "suspendu"  => Suspendu
    case "diplome"   => Diplome
    case _           => Inconnu
  }
}

// ─────────────────────────────────────────────
// Case class : Etudiant
// Identifiable → possède un id = matricule
// Affichable   → peut s'afficher en console
// Validable    → vérifie la cohérence de ses données
// ─────────────────────────────────────────────
case class Etudiant(
  matricule     : String,
  nom           : String,
  prenom        : String,
  sexe          : String,
  dateNaissance : String,
  email         : String,
  telephone     : String,
  filiere       : String,
  niveau        : String,
  annee         : String,
  statut        : StatutEtudiant
) extends Identifiable with Affichable with Validable {

  // Identifiable
  override def id: String = matricule

  // Affichable
  override def afficher(): Unit = {
    println(s"┌─────────────────────────────────────")
    println(s"│ Matricule : $matricule")
    println(s"│ Nom       : $prenom $nom")
    println(s"│ Filière   : $filiere  ($niveau)")
    println(s"│ Année     : $annee")
    println(s"│ Statut    : $statut")
    println(s"└─────────────────────────────────────")
  }

  // Validable
  override def estValide: Boolean = erreurs.isEmpty

  override def erreurs: List[String] = {
    List(
      if (matricule.isBlank)            Some("Matricule manquant")      else None,
      if (nom.isBlank)                  Some("Nom manquant")            else None,
      if (prenom.isBlank)               Some("Prénom manquant")         else None,
      if (!email.contains("@"))         Some("Email invalide")          else None,
      if (filiere.isBlank)              Some("Filière manquante")       else None,
      if (statut == Inconnu)            Some("Statut inconnu")          else None
    ).flatten
  }

  // Utilitaire : nom complet
  def nomComplet: String = s"$prenom $nom"
}

// ─────────────────────────────────────────────
// Objet compagnon : parsing depuis une ligne CSV
// ─────────────────────────────────────────────
object Etudiant {
  // matricule,nom,prenom,sexe,date_naissance,email,telephone,filiere,niveau,annee,statut
  def fromCSV(ligne: String): Option[Etudiant] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 11) None
    else Some(Etudiant(
      matricule     = cols(0),
      nom           = cols(1),
      prenom        = cols(2),
      sexe          = cols(3),
      dateNaissance = cols(4),
      email         = cols(5),
      telephone     = cols(6),
      filiere       = cols(7),
      niveau        = cols(8),
      annee         = cols(9),
      statut        = StatutEtudiant.fromString(cols(10))
    ))
  }
}
