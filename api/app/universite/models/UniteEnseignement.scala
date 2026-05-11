package universite.models

import universite.traits._

// ─────────────────────────────────────────────
// Case class : Unite d'Enseignement (UE)
// ─────────────────────────────────────────────
case class UniteEnseignement(
  idUE: String,
  nomUE: String,
  filiere: String,
  niveau: String,
  semestre: String,
  coefficientTotal: Int,
  matieres: List[String] = List.empty
) extends Identifiable with Affichable {
  
  override def id: String = idUE
  
  override def afficher(): Unit = {
    println(s"    [$idUE] $nomUE  (Coeff: $coefficientTotal)")
    println(s"        Filière: $filiere | Niveau: $niveau | Semestre: $semestre")
    println(s"        Matières: ${matieres.mkString(", ")}")
  }
  
  def nomComplet: String = s"$nomUE ($idUE)"
}

object UniteEnseignement {
  // id_ue,nom_ue,filiere,niveau,semestre,coefficient,matieres
  def fromCSV(ligne: String): Option[UniteEnseignement] = {
    val cols = ligne.split(",").map(_.trim)
    import scala.util.Try
    if (cols.length < 6) None
    else for {
      coeff <- Try(cols(5).toInt).toOption
    } yield {
      val matieres = if (cols.length >= 7 && cols(6).nonEmpty) 
        cols(6).split(";").toList 
      else 
        List.empty
      UniteEnseignement(
        idUE = cols(0),
        nomUE = cols(1),
        filiere = cols(2),
        niveau = cols(3),
        semestre = cols(4),
        coefficientTotal = coeff,
        matieres = matieres
      )
    }
  }
}

// ─────────────────────────────────────────────
// Case class : Semestre
// ─────────────────────────────────────────────
case class Semestre(
  idSemestre: String,
  nomSemestre: String,
  niveau: String,
  filiere: String,
  uniteEnseignements: List[String] = List.empty
) extends Identifiable with Affichable {
  
  override def id: String = idSemestre
  
  override def afficher(): Unit = {
    println(s"    [$idSemestre] $nomSemestre")
    println(s"        UEs: ${uniteEnseignements.mkString(", ")}")
  }
}

object Semestre {
  // id_semestre,nom_semestre,niveau,filiere,ues
  def fromCSV(ligne: String): Option[Semestre] = {
    val cols = ligne.split(",").map(_.trim)
    if (cols.length < 4) None
    else {
      val ues = if (cols.length >= 5 && cols(4).nonEmpty) 
        cols(4).split(";").toList 
      else 
        List.empty
      Some(Semestre(
        idSemestre = cols(0),
        nomSemestre = cols(1),
        niveau = cols(2),
        filiere = cols(3),
        uniteEnseignements = ues
      ))
    }
  }
}
