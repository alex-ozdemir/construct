package construct.semantics

import construct.engine.{SingleLocus, Union}
import construct.engine
import construct.input.ast.{Identifier, Pattern}
import construct.semantics.ConstructError.{BuiltinArity, BuiltinMisuse}
import construct.semantics.InternalError.{BuiltinBindArityError, BuiltinBindError, BuiltinBindValueError}
import construct.semantics.Value.Basic

import scala.collection.immutable.HashMap

object Builtins {

  val types: List[Type] =
    List(Circle(), Ray(), Line(), Segment())
  val typesMap: Map[Identifier, Type] = HashMap(types map { t =>
    t.id -> t
  }: _*)

  val functions: List[Function] = types ++ List(Intersection())
  val functionsMap: Map[Identifier, Function] = HashMap(functions map { f =>
    f.id -> f
  }: _*)


  sealed abstract class Function(val name: String, val arity: Int) extends Product with Serializable {
    val id: Identifier = Identifier(name)

    def execute(
        args: List[Value]): Value

    def check_arity(args: List[Value]): Unit = {
      if (arity != args.length)
        throw BuiltinArity(this, args)
    }

    //noinspection ZeroIndexToHead because we want to emphasize argument order
    def get_two_args(args: List[Value]): (Value, Value) = {
      check_arity(args)
      (args(0), args(1))
    }
  }

  case class Intersection() extends Function("intersection", 2) {
    override def execute(
        args: List[Value]): Value = {
      val (v1, v2) = get_two_args(args)
      if (v1 == v2) v1
      else
        v1.asLocus intersect v2.asLocus match {
          case locus: SingleLocus => Basic(locus)
          case Union(loci) =>
            ConstructInterpreter.mkValue(loci.toList map Basic)
        }
    }
  }

  sealed abstract class Type(name: String, arity: Int)
      extends Function(name, arity) with Product with Serializable {
    def check_subpats_arity(subpats: List[Pattern]): Either[BuiltinBindArityError, Unit] = {
      if (arity != subpats.length)
        Left(BuiltinBindArityError(this, subpats.length))
      else
        Right(())
    }

    //noinspection ZeroIndexToHead because we want to emphasize subpattern order
    def get_two_subpats(subpats: List[Pattern]): Either[BuiltinBindArityError,(Pattern, Pattern)] = {
      check_subpats_arity(subpats).right map { case () => (subpats(0), subpats(1))}
    }

    /**
      * Binds a pattern for this primitive type to that value, returning a list of subpatterns and values to bind
      *
      * @param subpats the subpatterns for this type
      * @param value   the value to bind
      * @return a bunch of subbindings to do
      */
    def bind(subpats: List[Pattern], value: Value): Either[BuiltinBindError, Iterator[(Pattern, Value)]]
  }

  case class Line() extends Type("line", 2) {
    override def execute(args: List[Value]): Value = {
      val (p1, p2) = get_two_args(args)
      if (p1 == p2)
        throw BuiltinMisuse("line", "identical points")
      Basic(engine.Line(p1.asPoint, p2.asPoint))
    }

    override def bind(subpats: List[Pattern],
                      value: Value): Either[BuiltinBindError, Iterator[(Pattern, Basic)]] = {
      get_two_subpats(subpats).right flatMap { case (pat1, pat2) =>

        value match {
          case Basic(engine.Line(p1, p2)) => Right(Iterator(pat1 -> Basic(p1), pat2 -> Basic(p2)))
          case _ => Left(BuiltinBindValueError(this, subpats, value))
        }
      }
    }
  }

  case class Segment() extends Type("segment", 2) {
    override def execute(args: List[Value]): Value = {
      val (p1, p2) = get_two_args(args)
      if (p1 == p2)
        throw BuiltinMisuse("segment", "identical points")
      Basic(engine.Segment(p1.asPoint, p2.asPoint))
    }

    override def bind(subpats: List[Pattern],
                      value: Value): Either[BuiltinBindError, Iterator[(Pattern, Basic)]] = {
      get_two_subpats(subpats).right flatMap { case (pat1, pat2) =>
        value match {
          case Basic(engine.Segment(p1, p2)) =>
            Right(Iterator(pat1 -> Basic(p1), pat2 -> Basic(p2)))
          case _ => Left(BuiltinBindValueError(this, subpats, value))
        }
      }
    }
  }

  case class Ray() extends Type("ray", 2) {
    override def execute(args: List[Value]): Value = {
      val (p1, p2) = get_two_args(args)
      if (p1 == p2)
        throw BuiltinMisuse("ray", "identical points")
      Basic(engine.Ray(p1.asPoint, p2.asPoint))
    }

    override def bind(subpats: List[Pattern],
                      value: Value): Either[BuiltinBindError, Iterator[(Pattern, Basic)]] = {
      get_two_subpats(subpats).right flatMap { case (pat1, pat2) =>
        value match {
          case Basic(engine.Ray(p1, p2)) =>
            Right(Iterator(pat1 -> Basic(p1), pat2 -> Basic(p2)))
          case _ => Left(BuiltinBindValueError(this, subpats, value))
        }
      }
    }
  }

  case class Circle() extends Type("circle", 2) {
    override def execute(args: List[Value]): Value = {
      val (c, e) = get_two_args(args)
      if (c == e)
        throw BuiltinMisuse("circle", "identical points")
      Basic(engine.Circle(c.asPoint, e.asPoint))
    }

    override def bind(subpats: List[Pattern],
                      value: Value): Either[BuiltinBindError, Iterator[(Pattern, Basic)]] = {
      get_two_subpats(subpats).right flatMap { case (pat1, pat2) =>
        value match {
          case Basic(engine.Circle(p1, p2)) =>
            Right(Iterator(pat1 -> Basic(p1), pat2 -> Basic(p2)))
          case _ => Left(BuiltinBindValueError(this, subpats, value))
        }
      }
    }
  }

}
