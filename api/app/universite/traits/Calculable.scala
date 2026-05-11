package universite.traits

trait Calculable {
  def calculerMoyenne(valeurs: List[Double]): Double =
    if (valeurs.isEmpty) 0.0
    else valeurs.sum / valeurs.size

  def sommeRecursive(valeurs: List[Double]): Double = valeurs match {
    case Nil          => 0.0
    case head :: tail => head + sommeRecursive(tail)
  }
}
