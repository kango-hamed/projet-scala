package universite.services

import universite.models._
import universite.repositories._
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import play.api.libs.json._
import java.time.Instant
import scala.util.{Try, Success, Failure}

// ─────────────────────────────────────────────
// Service : AuthService
// Gestion de l'authentification JWT et des mots de passe
// ─────────────────────────────────────────────
class AuthService @javax.inject.Inject()(
  userRepo: UtilisateurRepository
) {

  // ─── Configuration JWT ────────────────────
  private val SECRET_KEY = "votre-cle-super-secrete-changez-en-production"
  private val ALGORITHM = JwtAlgorithm.HS256
  private val TOKEN_EXPIRATION = 86400 // 24 heures en secondes

  // ─── Hashage des mots de passe (bcrypt) ─────

  def hasherMotDePasse(motDePasse: String): String =
    BCrypt.hashpw(motDePasse, BCrypt.gensalt(12))

  def verifierMotDePasse(motDePasse: String, hash: String): Boolean =
    BCrypt.checkpw(motDePasse, hash)

  // ─── Génération JWT ─────────────────────────

  def genererToken(utilisateur: Utilisateur): String = {
    val claim = JwtClaim(
      content = Json.obj(
        "id" -> utilisateur.idUtilisateur,
        "email" -> utilisateur.email,
        "role" -> utilisateur.role.code,
        "idProfil" -> utilisateur.idProfil
      ).toString
    )
      .issuedAt(Instant.now().getEpochSecond)
      .expiresAt(Instant.now().getEpochSecond + TOKEN_EXPIRATION)

    Jwt.encode(claim, SECRET_KEY, ALGORITHM)
  }

  // ─── Vérification JWT ───────────────────────

  def verifierToken(token: String): Option[JwtClaim] = {
    Jwt.decode(token, SECRET_KEY, Seq(ALGORITHM)) match {
      case Success(claim) => Some(claim)
      case Failure(_) => None
    }
  }

  def extraireUtilisateurDuToken(token: String): Option[Utilisateur] = {
    verifierToken(token).flatMap { claim =>
      val json = Json.parse(claim.content)
      (json \ "id").asOpt[String].flatMap(id => userRepo.trouverParId(id))
    }
  }

  // ─── Authentification ───────────────────────

  def authentifier(email: String, motDePasse: String): Either[String, (Utilisateur, String)] = {
    userRepo.trouverParEmail(email) match {
      case None => Left("Email ou mot de passe incorrect")
      case Some(utilisateur) if !utilisateur.actif =>
        Left("Compte suspendu. Contactez l'administrateur.")
      case Some(utilisateur) =>
        if (verifierMotDePasse(motDePasse, utilisateur.motDePasseHash)) {
          val token = genererToken(utilisateur)
          Right((utilisateur, token))
        } else {
          Left("Email ou mot de passe incorrect")
        }
    }
  }

  // ─── Inscription ────────────────────────────

  def inscrire(
    email: String,
    motDePasse: String,
    role: RoleUtilisateur,
    idProfil: String
  ): Either[String, Utilisateur] = {
    // Validations
    if (email.isBlank || !email.contains("@")) {
      return Left("Email invalide")
    }
    if (motDePasse.length < 6) {
      return Left("Le mot de passe doit contenir au moins 6 caractères")
    }
    if (userRepo.emailExiste(email)) {
      return Left("Cet email est déjà utilisé")
    }

    // Créer l'utilisateur
    val id = genererIdUtilisateur(role, idProfil)
    val hash = hasherMotDePasse(motDePasse)
    val nouvelUtilisateur = Utilisateur(
      idUtilisateur = id,
      email = email,
      motDePasseHash = hash,
      role = role,
      idProfil = idProfil,
      actif = true
    )

    if (userRepo.sauvegarder(nouvelUtilisateur)) {
      Right(nouvelUtilisateur)
    } else {
      Left("Erreur lors de la création du compte")
    }
  }

  // ─── Helpers ────────────────────────────────

  private def genererIdUtilisateur(role: RoleUtilisateur, idProfil: String): String = {
    val prefix = role.code match {
      case "ADMIN" => "ADM"
      case "ENSEIGNANT" => "ENS"
      case "ETUDIANT" => "ETU"
      case _ => "USR"
    }
    s"${prefix}_USR_${idProfil}"
  }

  // ─── Méthodes pour les contrôleurs ──────────

  def extraireTokenHeader(header: String): Option[String] = {
    header match {
      case h if h.startsWith("Bearer ") => Some(h.substring(7))
      case _ => None
    }
  }

  def obtenirUtilisateurConnecte(tokenOpt: Option[String]): Option[Utilisateur] = {
    tokenOpt.flatMap(extraireUtilisateurDuToken)
  }
}
