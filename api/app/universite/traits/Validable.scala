package universite.traits

trait Validable {
  def estValide: Boolean
  def erreurs: List[String]
}
