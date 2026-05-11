package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Enum : Rôle utilisateur
// ─────────────────────────────────────────────
sealed trait RoleUtilisateur {
  def code: String
  def nom: String
}

case object RoleAdmin       extends RoleUtilisateur { val code = "ADMIN";       val nom = "Administrateur" }
case object RoleEnseignant  extends RoleUtilisateur { val code = "ENSEIGNANT";  val nom = "Enseignant" }
case object RoleEtudiant    extends RoleUtilisateur { val code = "ETUDIANT";    val nom = "Etudiant" }

object RoleUtilisateur {
  def fromString(s: String): RoleUtilisateur = s.trim.toUpperCase match {
    case "ADMIN" | "ADMINISTRATEUR" => RoleAdmin
    case "ENSEIGNANT" | "PROF" => RoleEnseignant
    case "ETUDIANT" | "STUDENT" => RoleEtudiant
    case _ => RoleEtudiant
  }
  
  def tous: List[RoleUtilisateur] = List(RoleAdmin, RoleEnseignant, RoleEtudiant)
}

// ─────────────────────────────────────────────
// Case class : Utilisateur (compte)
// ─────────────────────────────────────────────
case class Utilisateur(
  idUtilisateur: String,
  email: String,
  motDePasseHash: String,  // Stocké hashé (bcrypt)
  role: RoleUtilisateur,
  idProfil: String,          // Matricule pour étudiant, ID enseignant, ou "ADMIN"
  actif: Boolean = true
) extends Identifiable with Affichable {
  
  override def id: String = idUtilisateur
  
  override def afficher(): Unit = {
    val statut = if (actif) "Actif" else "Suspendu"
    println(s"┌─────────────────────────────────────")
    println(s"│ ID: $idUtilisateur")
    println(s"│ Email: $email")
    println(s"│ Rôle: ${role.nom}")
    println(s"│ Profil: $idProfil")
    println(s"│ Statut: $statut")
    println(s"└─────────────────────────────────────")
  }
  
  def estActif: Boolean = actif
  def estAdmin: Boolean = role == RoleAdmin
  def estEnseignant: Boolean = role == RoleEnseignant
  def estEtudiant: Boolean = role == RoleEtudiant
}

object Utilisateur {
  // id_utilisateur,email,password_hash,role,id_profil,actif
  def fromCSV(ligne: String): Option[Utilisateur] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 5) None
    else {
      val actif = if (cols.length >= 6) cols(5).toBooleanOption.getOrElse(true) else true
      Some(Utilisateur(
        idUtilisateur = cols(0),
        email = cols(1),
        motDePasseHash = cols(2),
        role = RoleUtilisateur.fromString(cols(3)),
        idProfil = cols(4),
        actif = actif
      ))
    }
  }
}
