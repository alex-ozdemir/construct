package construct.semantics

import construct.engine.{Locus, Point, SingleLocus}
import construct.input.ast.Identifier
import construct.semantics.ConstructError.TypeError

sealed abstract class Value {
  def asPoint: Point = {
    this match {
      case Value.Basic(p: Point) => p
      case _                     => throw TypeError(this, "point")
    }
  }
  def asLocus: Locus = {
    this match {
      case Value.Basic(v)        => v
      case Value.Custom(_, _, v) => v
      case Value.Product(_, v)   => v
    }
  }
  def pretty: String
  def pointsIterator: Iterator[Point] = asLocus.pointsIterator()
}

object Value {
  case class Basic(v: SingleLocus) extends Value {
    def pretty: String = v.name
  }

  case class Custom(ty: Identifier, params: List[Value], v: Locus)
      extends Value {
    def pretty: String = ty.name
  }
  case class Product(params: List[Value], v: Locus) extends Value {
    def pretty: String = params map { _.pretty } mkString ("(", ", ", ")")

    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case Product(_, u) => u == v
        case _             => false
      }
    }
  }
}
