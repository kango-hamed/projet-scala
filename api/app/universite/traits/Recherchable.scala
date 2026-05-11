package universite.traits

trait Recherchable[A] {
  def trouverParId(id: String, liste: List[A]): Option[A]
}
