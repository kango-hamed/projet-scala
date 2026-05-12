package universite.services

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import scala.jdk.CollectionConverters._

// ─────────────────────────────────────────────
// Service : RateLimiterService
// Protection contre brute-force attacks
// ─────────────────────────────────────────────
@Singleton
class RateLimiterService {
  
  private val MAX_ATTEMPTS = 35         // Tentatives max par fenêtre
  private val WINDOW_SECONDS = 900       // Fenêtre de 15 minutes
  private val BLOCK_DURATION = 600      // Blocage 1 heure
  
  // Structure: ip → (tentatives, timestampDerniereTentative, bloqueJusqua)
  private val attempts = new ConcurrentHashMap[String, (Int, Long, Long)]()
  
  // ─── Vérifier si l'IP peut tenter une connexion ───
  def peutTenter(ip: String): Boolean = {
    val now = Instant.now().getEpochSecond
    
    attempts.get(ip) match {
      case null => true  // Pas d'historique
      case (_, _, blockedUntil) if blockedUntil > now => 
        false  // Toujours bloqué
      case (count, lastAttempt, _) =>
        // Si fenêtre expirée, reset
        if (now - lastAttempt > WINDOW_SECONDS) {
          attempts.remove(ip)
          true
        } else {
          count < MAX_ATTEMPTS
        }
    }
  }
  
  // ─── Enregistrer une tentative échouée ────────────
  def enregistrerEchec(ip: String): Unit = {
    val now = Instant.now().getEpochSecond
    
    attempts.compute(ip, (_, existing) => {
      val (count, _, _) = Option(existing).getOrElse((0, 0L, 0L))
      val newCount = count + 1
      val blockedUntil = if (newCount >= MAX_ATTEMPTS) now + BLOCK_DURATION else 0L
      (newCount, now, blockedUntil)
    })
  }
  
  // ─── Enregistrer un succès (reset le compteur) ────
  def enregistrerSucces(ip: String): Unit = {
    attempts.remove(ip)
  }
  
  // ─── Temps restant avant déblocage ───────────────
  def tempsAttente(ip: String): Long = {
    val now = Instant.now().getEpochSecond
    Option(attempts.get(ip)) match {
      case Some((_, _, blockedUntil)) if blockedUntil > now => blockedUntil - now
      case _ => 0
    }
  }
  
  // ─── Message d'erreur formaté ─────────────────────
  def messageErreur(ip: String): String = {
    val attente = tempsAttente(ip)
    if (attente > 0) {
      s"Trop de tentatives. Réessayez dans ${attente / 60} minutes."
    } else {
      val restantes = MAX_ATTEMPTS - Option(attempts.get(ip)).map(_._1).getOrElse(0)
      s"Email ou mot de passe incorrect ($restantes tentatives restantes)"
    }
  }
}
