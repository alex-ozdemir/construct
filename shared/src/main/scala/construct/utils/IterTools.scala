package construct.utils

object IterTools {
  def cartesianProduct[T](xss: List[Iterable[T]]): Iterable[List[T]] =
    xss match {
      case Nil    => List(Nil)
      case h :: t => for (xh <- h; xt <- cartesianProduct(t)) yield xh :: xt
    }

  def uniqueBy[A, B](list: Iterable[A], fn: Function1[A, B]): List[A] = {
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
