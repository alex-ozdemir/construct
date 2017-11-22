// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the interpreter for Construct
package construct.semantics

import construct.engine._
import construct.input.ast._
import construct.output.{Drawable, PrettyPrinter}

import scala.collection.mutable
import scala.xml.parsing.ConstructingParser

sealed abstract class Var {
  def asPoint: Point = {
    this match {
      case Basic(p: Point) => p
      case _               => throw TypeError(this, "point")
    }
  }
  def asLocus: Locus = {
    this match {
      case Basic(v)        => v
      case Custom(_, _, v) => v
      case Product(_, v)   => v
    }
  }
  def pretty: String
}

case class Basic(v: SingleLocus) extends Var {
  def pretty: String = v.name
}

case class Custom(ty: Identifier, params: List[Var], v: Locus)
    extends Var {
  def pretty: String = ty.name
}
case class Product(params: List[Var], v: Locus) extends Var {
  def pretty: String = params map {_.pretty} mkString("(",", ",")")

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case Product(_, v) => v == this.v
      case _ => false
    }
  }
}

abstract class ConstructError(val msg: String)
    extends RuntimeException(s"Error: $msg")
case class UnknownIdentifier(id: Identifier)
    extends ConstructError(s"Unkown identifier `${id.name}`")
case class UnknownIdentifierOfType(id: Identifier, ty: String)
    extends ConstructError(s"Unkown identifier `${id.name}` of type `$ty`")
case class TypeError(v: Var, expected: String)
    extends ConstructError(s"<${v.pretty}> was expected to be a `$expected`!")
case class ParameterTypeError(param: Parameter, v: Var)
    extends ConstructError(
      s"The formal parameter `${param.name.name}` is supposed to have type `${param.ty}` but the actual parameter was <${v.pretty}>!")
case class UsedIdentifier(id: String)
    extends ConstructError(s"The identifier `$id` has already been used")
case class SelfIntersection(fn: FnApp, v: Var)
    extends ConstructError(s"The function application `${PrettyPrinter.print(fn)}` is trying to intersect <${v.pretty}> with itself")
case class BuiltinMisuseError(ty: String, from: String)
    extends ConstructError(s"Cannot construct a `$ty` from $from")
case class ImplicitGiven(param: Parameter)
    extends ConstructError(
      s"Implicit givens must be points, but `${PrettyPrinter.print(param.name)}` is a `${param.ty}`")
case class RebindError(pattern: Pattern, ident: Identifier)
    extends ConstructError(s"The pattern `${PrettyPrinter.print(pattern)}` rebinds the variable `${ident.name}`")
case class ArityError(c: Construction, vars: List[Var])
    extends ConstructError(
      s"The construction `${c.name.name}` expects ${c.parameters.length} parameters but got ${vars.length}")
case class BuiltinArityError(b: Builtins.Function, vars: List[Var])
    extends ConstructError(
      s"The builtin `${b.name}` expects ${b.arity} parameters but got ${vars.length}")
case class BuiltinBindError(b: Builtins.Type, pats: List[Pattern])
  extends ConstructError(
    s"The builtin `${b.name}` can be broken into ${b.arity} points, but you tried to break it into ${pats.length} items")
abstract class BindError(value_ty: String, v: Var, p: Pattern, reason: String) extends ConstructError(
  s"Cannot bind the $value_ty <${v.pretty}> to the pattern `${PrettyPrinter.print(p)}. $reason"
)
case class ProductBindError(union: Tuple, prod: Product)
    extends BindError("ordered union", prod, union, "The number of loci disagree")
case class DestructorBindArityError(des: Destructor, v: Var)
    extends BindError("value", v, des, "The arities disagree")
case class DestructorBindTypeError(des: Destructor, v: Var)
    extends BindError("value", v, des, "The types disagree")
case class VagueBindError(pat: Pattern, v: Var)
    extends BindError("value", v, pat, "")
case class FileNotFound(error: String) extends ConstructError(error)
case class IncludeError(filename: String, error: String)
    extends ConstructError(s"While reading '$filename' I encountered the following error:\n$error")
case class WebInclude(file: String)
    extends ConstructError(s"I cannot include `$file`, as web construct does not support includes")
class ConstructInterpreter {

  val constructions = new mutable.HashMap[Identifier, Construction]
  val constructors = new mutable.HashMap[Identifier, Construction]
  val vars = new mutable.HashMap[Identifier, Var]
  val def_points =
    mutable.Queue(Point(0.0, 0.0), Point(1.0, 0.0), Point(1.7, 1.0), Point(2.0, 0.5))
  private val builtins =
    List("circle", "line", "segment", "ray", "intersection") map {
      Identifier
    }

  def checkFresh(id: Identifier): Unit =
    if (vars.keys exists { _ == id }) throw UsedIdentifier(id.name)

  def lookupPoint(id: Identifier): Point =
    vars.getOrElse(id, {
      throw UnknownIdentifierOfType(id, "point")
    }).asPoint

  def lookupConstructor(id: Identifier): Construction =
    constructors.getOrElse(id, {
      throw UnknownIdentifier(id)
    })

  def lookupConstruction(id: Identifier): Construction =
    constructions.getOrElse(id, {
      throw UnknownIdentifier(id)
    })

  def lookupVar(id: Identifier): Var =
    vars.getOrElse(id, {
      throw UnknownIdentifier(id)
    })

  def lookupLocus(id: Identifier): Locus =
    lookupVar(id).asLocus

  def mkProduct(params: List[Var]): Product = {
    Product(params, params.map {
      _.asLocus
    }.foldLeft(Union(Set()): Locus){ _ union _ })
  }

  def run(c: Construction,
          items: Iterable[Item],
          in_vars: Option[List[Var]] = None): Var = {
    val Construction(_, params, statements, outs) = c
    set_inputs(params, in_vars)
    add_items(items)
    statements foreach { execute }
    val vars = outs map { lookupVar }
    if (vars.length > 1) {
      val loci = vars map { _.asLocus }
      val locus = loci reduce { _ union _ }
      Product(vars, locus)
    } else if (vars.length == 1) vars.head
    else Product(List(),Union(Set()))
  }

  def add_items(items: Iterable[Item]): Unit = {
    constructions ++= (items collect { case c: Construction => (c.name, c) })
    constructors ++= (items collect { case Shape(c)         => (c.name, c) })
  }

  def add_input(param: Parameter, actual_var: Option[Var]): Unit = {
    param match {
      case Parameter(id, ty) => {
        actual_var match {
          case None => {
            if (ty != Identifier("point"))
              throw ImplicitGiven(param)
            val pt = def_points.dequeue()
            vars += (id -> Basic(pt))
          }
          case Some(v) => {
            if (getTy(v) != ty)
              throw ParameterTypeError(param, v)
            vars += ((id, v))
          }
        }
      }
    }
  }

  def set_inputs(params: Iterable[Parameter], in_vars: Option[List[Var]]): Unit = {
    in_vars match {
      case None => params foreach { add_input(_, None) }
      case Some(ins_list) =>
        params zip ins_list foreach {
          case (param, in) => add_input(param, Some(in))
        }
    }
  }

  def getTy(v: Var): Identifier = {
    Identifier(v match {
      case Basic(_: Line)    => "line"
      case Basic(_: Circle)  => "circle"
      case Basic(_: Segment) => "segment"
      case Basic(_: Point)   => "point"
      case Basic(_: Ray)     => "ray"
      case _: Product        => "ordered union"
      case Custom(t, _, _)   => t.name
    })
  }

  def intersection(fn: FnApp, v1: Var, v2: Var): Var = {
    if (v1 == v2) throw SelfIntersection(fn, v1)
    v1.asLocus intersect v2.asLocus match {
      case locus: SingleLocus => Basic(locus)
      case Union(loci) => mkProduct(loci.toList map { Basic })
    }
  }

  def construct_line(p1: Var, p2: Var): Var = {
    if (p1 == p2)
      throw BuiltinMisuseError("line", "identical points")
    Basic(Line(p1.asPoint, p2.asPoint))
  }

  def construct_circle(c: Var, e: Var): Var = {
    if (c == e)
      throw BuiltinMisuseError("circle", "identical points")
    Basic(Circle(c.asPoint, e.asPoint))
  }

  def construct_ray(p1: Var, p2: Var): Var = {
    if (p1 == p2)
      throw BuiltinMisuseError("ray", "identical points")
    Basic(Ray(p1.asPoint, p2.asPoint))
  }

  def construct_segment(p1: Var, p2: Var): Var = {
    if (p1 == p2)
      throw BuiltinMisuseError("segment", "identical points")
    Basic(Segment(p1.asPoint, p2.asPoint))
  }

  def fn_call(fn: Identifier, ins: List[Var]): Var = {
    if (constructors contains fn) constructor_call(fn, ins)
    else construction_call(fn, ins)
  }

  def procedure_call(fn: Identifier,
                     ins: List[Var],
                     lookup: Identifier => Construction): Var = {
    val con = lookup(fn)
    val cons_in_new_env = constructions.values filter { _ != con }
    val shapes_in_new_env = constructors.values filter { _ != con } map {
      Shape
    }
    val env_cons = cons_in_new_env ++ shapes_in_new_env
    val con_in_count = con.parameters.length
    check_arg_count(con, ins)
    val call_eval = new ConstructInterpreter
    call_eval.run(con, env_cons, Some(ins))
  }

  def constructor_call(fn: Identifier, ins: List[Var]): Var =
    Custom(fn, ins, procedure_call(fn, ins, lookupConstructor).asLocus)

  def construction_call(fn: Identifier, ins: List[Var]): Var =
    procedure_call(fn, ins, lookupConstruction)

  def check_arg_count(c: Construction, vars: List[Var]): Unit =
    if (vars.length != c.parameters.length)
      throw ArityError(c, vars)

  def builtin_check_arg_count(b: Builtins.Function, vars: List[Var]): Unit =
    if (b.arity != vars.length)
      throw BuiltinArityError(b, vars)

  def fn_evaluate(fn_app: FnApp): Var = {
    val FnApp(fn, arg_exprs) = fn_app
    val arg_vars = arg_exprs map { evaluate }
    lazy val arg0 = arg_vars(0)
    lazy val arg1 = arg_vars(1)
    fn match {
      case Identifier("intersection") => {
        builtin_check_arg_count(Builtins.Intersection(), arg_vars)
        intersection(fn_app, arg0, arg1)
      }
      case Identifier("circle") => {
        builtin_check_arg_count(Builtins.Circle(), arg_vars)
        construct_circle(arg0, arg1)
      }
      case Identifier("line") => {
        builtin_check_arg_count(Builtins.Line(), arg_vars)
        construct_line(arg0, arg1)
      }
      case Identifier("ray") => {
        builtin_check_arg_count(Builtins.Ray(), arg_vars)
        construct_ray(arg0, arg1)
      }
      case Identifier("segment") => {
        builtin_check_arg_count(Builtins.Segment(), arg_vars)
        construct_segment(arg0, arg1)
      }
      case fn_id => fn_call(fn_id, arg_vars)
    }
  }

  def evaluate(expr: Expr): Var = {
    expr match {
      case fn_app: FnApp => fn_evaluate(fn_app)
      case Exactly(id)   => lookupVar(id)
      case SetLit(exprs) => mkProduct(exprs map evaluate)
      case Difference(left, right) =>
        difference(evaluate(left), evaluate(right))
    }
  }

  def difference(left: Var, right: Var): Var = {
    def mkVar(vs: List[Var]): Var =
      vs match {
        case List(x) => x
        case xs      => mkProduct(xs)
      }
    (left, right) match {
      case (Product(left_vs, _), Product(right_vs, _)) =>
        mkVar(left_vs filter { v =>
          !(right_vs contains v)
        })
      case (v, _) => v
    }
  }

  def query(fn_id: Identifier): Iterable[(List[Drawable], Expr, String)] = {
    val n_params =
      if (builtins contains fn_id) 2
      else lookupConstruction(fn_id).parameters.length

    def try_call(fn_app: FnApp): Option[Var] = {
      try { Some(fn_evaluate(fn_app)) } catch { case _: ConstructError => None }
    }

    val possible_inputs = vars.keys map { Exactly }
    val params =
      IterTools.cartesianProduct(((1 to n_params) map { n =>
        possible_inputs
      }).toList)
    val possible_calls = params map { FnApp(fn_id, _) }
    val results = possible_calls map { call =>
      (try_call(call), call)
    }
    val filteredResults = filter_query_results(results)
    filteredResults.zipWithIndex map {
      case ((v, expr), i) =>
        (make_named_split_set(i.toString, v), expr, i.toString)
    }
  }

  def filter_query_results(
      results: Iterable[(Option[Var], Expr)]): Iterable[(Var, Expr)] = {
    val extantResults = results collect {
      case (Some(objs), call) => (objs, call)
    }
    val nonEmptyResults = extantResults filter {
      case (Product(_, locus), _) => !locus.empty
      case _                       => true
    }
    val newResults = nonEmptyResults filter {
      case (Product(_, Union(loci)), _) => (loci.iterator count { s => (vars.values count { Basic(s) == _ }) == 0 }) > 0
      case (Product(_, locus: PrimativeLocus), _) => (vars.values count {Basic(locus) == _}) == 0
      case (v, _) => (vars.values count { v == _ }) == 0
    }
    IterTools.uniqueBy(newResults, { x: (Var, Expr) =>
      x._1
    })
  }

  def execute(assignment: Statement): Traversable[Identifier] = {
    val Statement(pattern, expr) = assignment
    val boundIndents = pattern.boundIdents.toSet
    val reboundIdents = boundIndents intersect vars.keySet
    if (reboundIdents.nonEmpty) {
      throw RebindError(pattern, reboundIdents.iterator.next())
    }
    pattern_match(pattern, evaluate(expr))
    boundIndents
  }

  def make_named(id: Identifier, v: Var): Drawable =
    Drawable(id.name, v.asLocus)

  def make_named_split_set(id: String, v: Var): List[Drawable] =
    v match {
      case Basic(locus)        => List(Drawable(id, locus))
      case Custom(_, _, locus) => List(Drawable(id, locus))
      case Product(_, Union(s))   => s.toList flatMap { l =>
        make_named_split_set(id, Basic(l))
      }
      case Product(_, locus)   => List(Drawable(id, locus))
    }

  def get_drawables: Iterable[Drawable] = vars map {
    case (id, v) => make_named(id, v)
  }

  def pattern_match(pattern: Pattern, v: Var): Unit = {
    (pattern, v) match {
      case (Id(id), v) => vars += (id -> v)
      case (u @ Tuple(pats), prod@Product(vars, _)) => {
        if (pats.length != vars.toList.length)
          throw ProductBindError(u, prod)
        pats zip vars.toList foreach Function.tupled(pattern_match)
      }
      case (d@Destructor(Identifier("circle"), pats), Basic(Circle(c, e))) => {
        if (pats.size != 2) {
          throw BuiltinBindError(Builtins.Circle(), pats)
        }
        pattern_match(pats.head, Basic(c))
        pattern_match(pats.tail.head, Basic(e))
      }
      case (d@Destructor(Identifier("line"), pats), Basic(Line(c, e))) => {
        if (pats.size != 2) {
          throw BuiltinBindError(Builtins.Line(), pats)
        }
        pattern_match(pats.head, Basic(c))
        pattern_match(pats.tail.head, Basic(e))
      }
      case (d@Destructor(Identifier("ray"), pats), Basic(Ray(c, e))) => {
        if (pats.size != 2) {
          throw BuiltinBindError(Builtins.Ray(), pats)
        }
        pattern_match(pats.head, Basic(c))
        pattern_match(pats.tail.head, Basic(e))
      }
      case (d@Destructor(Identifier("segment"), pats), Basic(Segment(c, e))) => {
        if (pats.size != 2) {
          throw BuiltinBindError(Builtins.Segment(), pats)
        }
        pattern_match(pats.head, Basic(c))
        pattern_match(pats.tail.head, Basic(e))
      }
      case (s @ Destructor(ty1, pats), c @ Custom(ty2, vars, _)) => {
        if (ty1 != ty2) throw DestructorBindTypeError(s, c)
        if (pats.length != vars.toList.length)
          throw DestructorBindArityError(s, c)
        pats zip vars.toList foreach Function.tupled(pattern_match)
      }
      case (pat, v) => {
        throw VagueBindError(pat, v)
      }
    }
  }

  override def toString: String =
    "Variables:\n" +
      (vars map { case (k, v) => k.toString + " => " + v.toString } mkString "\n") +
      "\nConstructions:\n" +
      (constructions map { case (k, v) => k.toString + " => " + " ... " } mkString "\n") +
      "\nConstructors:\n" +
      (constructors map { case (k, v) => k.toString + " => " + " ... " } mkString "\n")
}

object Builtins {

  sealed abstract class Function(val name: String, val arity: Int) {
    val id: Identifier = Identifier(name)
  }
  case class Intersection() extends Function("intersect", 2)

  sealed abstract class Type(override val name: String, override val arity: Int)
      extends Function(name, arity) {}
  case class Line() extends Type("line", 2)
  case class Segment() extends Type("segment", 2)
  case class Ray() extends Type("ray", 2)
  case class Circle() extends Type("circle", 2)
}

object IterTools {
  def cartesianProduct[T](xss: List[Iterable[T]]): Iterable[List[T]] =
    xss match {
      case Nil    => List(Nil)
      case h :: t => for (xh <- h; xt <- cartesianProduct(t)) yield xh :: xt
    }
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
