package universite.repositories

import universite.traits._
import scala.util.{Try, Success, Failure}
import scala.io.Source

// ─────────────────────────────────────────────
// Trait générique : BaseRepository[A]
// Facttorise la lecture CSV pour tous les repos
// ─────────────────────────────────────────────
trait BaseRepository[A] {

  // Chemin du fichier CSV (défini par chaque repo)
  def cheminFichier: String

  // Fonction de parsing d'une ligne → Option[A]
  def parseLigne(ligne: String): Option[A]

  // Lecture complète du fichier CSV
  // Retourne un Try[List[A]] pour gérer les erreurs d'I/O
  def chargerTout(): Try[List[A]] = Try {
    val file = new java.io.File(cheminFichier)
    // Si le fichier n'existe pas localement, on tente de le chercher dans le dossier parent (cas où on lance depuis 'api/')
    val actualFile = if (file.exists()) file else {
      val parentFile = new java.io.File("../" + cheminFichier)
      if (parentFile.exists()) parentFile else file
    }

    val source = Source.fromFile(actualFile)
    try {
      source.getLines()
        .drop(1)              // ignorer l'entête CSV
        .filter(_.nonEmpty)   // ignorer les lignes vides
        .flatMap(parseLigne)  // parser chaque ligne (Option → filtrage automatique des None)
        .toList
    } finally {
      source.close()
    }
  }

  // Version sûre : retourne une liste vide en cas d'erreur
  def chargerOuVide(): List[A] = chargerTout() match {
    case Success(liste) => liste
    case Failure(ex)    =>
      println(s"[ERREUR] Impossible de lire $cheminFichier : ${ex.getMessage}")
      List.empty
  }

  // Recherche par ID (retourne Option[A])
  def trouverParId(id: String, extraireId: A => String): Option[A] =
    chargerOuVide().find(a => extraireId(a) == id)
}
