package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Enum-like : Niveau d'études
// ─────────────────────────────────────────────
sealed trait NiveauEtudes {
  def nom: String
  def ordre: Int
}

case object Licence1 extends NiveauEtudes { val nom = "Licence 1"; val ordre = 1 }
case object Licence2 extends NiveauEtudes { val nom = "Licence 2"; val ordre = 2 }
case object Licence3 extends NiveauEtudes { val nom = "Licence 3"; val ordre = 3 }
case object Master1  extends NiveauEtudes { val nom = "Master 1";  val ordre = 4 }
case object Master2  extends NiveauEtudes { val nom = "Master 2";  val ordre = 5 }

object NiveauEtudes {
  def fromString(s: String): NiveauEtudes = s.trim.toLowerCase match {
    case "licence 1" | "l1" | "licence1" => Licence1
    case "licence 2" | "l2" | "licence2" => Licence2
    case "licence 3" | "l3" | "licence3" => Licence3
    case "master 1"  | "m1" | "master1"  => Master1
    case "master 2"  | "m2" | "master2"  => Master2
    case _ => Licence1
  }
  
  def tous: List[NiveauEtudes] = List(Licence1, Licence2, Licence3, Master1, Master2)
}

// ─────────────────────────────────────────────
// Case class : Niveau (lien filière + niveau)
// ─────────────────────────────────────────────
case class Niveau(
  idNiveau: String,
  filiere: String,
  niveauEtudes: NiveauEtudes,
  semestres: List[String] = List.empty
) extends Identifiable with Affichable {
  
  override def id: String = idNiveau
  
  override def afficher(): Unit = {
    println(s"  [$idNiveau] ${filiere} - ${niveauEtudes.nom}")
    println(s"      Semestres: ${semestres.mkString(", ")}")
  }
  
  def nomComplet: String = s"${filiere} - ${niveauEtudes.nom}"
}

object Niveau {
  // id_niveau,filiere,niveau,semestres
  def fromCSV(ligne: String): Option[Niveau] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 3) None
    else {
      val semestres = if (cols.length >= 4 && cols(3).nonEmpty) 
        cols(3).split(";").toList 
      else 
        List.empty
      Some(Niveau(
        idNiveau = cols(0),
        filiere = cols(1),
        niveauEtudes = NiveauEtudes.fromString(cols(2)),
        semestres = semestres
      ))
    }
  }
}
