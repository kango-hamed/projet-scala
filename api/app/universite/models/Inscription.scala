package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Enum-like : Statut d'une inscription
// ─────────────────────────────────────────────
sealed trait StatutInscription
case object Validee   extends StatutInscription
case object EnAttente extends StatutInscription
case object Annulee   extends StatutInscription

object StatutInscription {
  def fromString(s: String): StatutInscription = s.trim.toLowerCase match {
    case "validee"    | "validée"    => Validee
    case "en attente"                => EnAttente
    case "annulee"    | "annulée"    => Annulee
    case _                           => EnAttente
  }
}

// ─────────────────────────────────────────────
// Case class : Inscription
// Validable → empêche la double inscription
// ─────────────────────────────────────────────
case class Inscription(
  idInscription : String,
  matricule     : String,
  filiere       : String,
  niveau        : String,
  annee         : String,
  statut        : StatutInscription
) extends Identifiable with Affichable with Validable {

  override def id: String = idInscription

  override def afficher(): Unit =
    println(s"  [$idInscription] $matricule  →  $filiere ($niveau)  |  $annee  |  $statut")

  override def estValide: Boolean = erreurs.isEmpty

  override def erreurs: List[String] = List(
    if (matricule.isBlank) Some("Matricule manquant")  else None,
    if (filiere.isBlank)   Some("Filière manquante")   else None,
    if (annee.isBlank)     Some("Année manquante")     else None
  ).flatten
}

object Inscription {
  // id_inscription,matricule,filiere,niveau,annee,statut
  def fromCSV(ligne: String): Option[Inscription] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 6) None
    else Some(Inscription(
      idInscription = cols(0),
      matricule     = cols(1),
      filiere       = cols(2),
      niveau        = cols(3),
      annee         = cols(4),
      statut        = StatutInscription.fromString(cols(5))
    ))
  }
}

// ─────────────────────────────────────────────
// Case class : SeanceCours (Emploi du temps)
// ─────────────────────────────────────────────
case class SeanceCours(
  idSeance     : String,
  idMatiere    : String,
  idEnseignant : String,
  idSalle      : String,
  jour         : String,
  heureDebut   : String,
  heureFin     : String,
  filiere      : String,
  niveau       : String
) extends Identifiable with Affichable {

  override def id: String = idSeance

  override def afficher(): Unit =
    println(s"  [$idSeance] $jour $heureDebut-$heureFin  |  $idMatiere  |  Salle: $idSalle  |  $filiere ($niveau)")

  // Vérifie si deux séances sont en conflit (même salle, même jour, chevauchement horaire)
  def enConflit(autre: SeanceCours): Boolean =
    this.idSalle == autre.idSalle &&
    this.jour    == autre.jour    &&
    this.idSeance != autre.idSeance &&
    !(this.heureFin <= autre.heureDebut || autre.heureFin <= this.heureDebut)
}

object SeanceCours {
  // id_seance,matiere,enseignant,salle,jour,heure_debut,heure_fin,filiere,niveau
  def fromCSV(ligne: String): Option[SeanceCours] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 9) None
    else Some(SeanceCours(cols(0), cols(1), cols(2), cols(3), cols(4), cols(5), cols(6), cols(7), cols(8)))
  }
}
