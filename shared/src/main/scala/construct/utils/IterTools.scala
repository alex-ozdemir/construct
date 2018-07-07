package construct.utils

object IterTools {
  def cartesianProduct[T](xss: List[Iterable[T]]): Iterable[List[T]] =
    xss match {
      case Nil    => List(Nil)
      case h :: t => for (xh <- h; xt <- cartesianProduct(t)) yield xh :: xt
    }

  /**
    * Given a
    * @param list of items and a
    * @param fn from items to key values
    * @return a list of the items such that no two have the same key, and the first value having each key is present.
    */
  def uniqueBy[A, B](list: Iterable[A], fn: A => B): List[A] = {
    val bs = scala.collection.mutable.MutableList[B]()
    val out = scala.collection.mutable.MutableList[A]()
    for (a <- list) {
      val f = fn(a)
      if (!(bs contains f)) {
        bs += f
        out += a
      }
    }
    out.toList
  }
}
