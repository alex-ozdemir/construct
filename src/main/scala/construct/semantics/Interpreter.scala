// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the interpreter for Construct
package construct.semantics

import construct.engine._
import construct.input.ast._
import construct.output.Drawable
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap

sealed abstract class Var {
  def asPoint : Point = {
    this match {
      case Basic(p : Point) => p
      case _ => throw new TypeError(this, "point")
    }
  }
  def asLocus : Locus = {
    this match {
      case Basic(v) => v
      case Custom(_,_,v) => v
      case Product(_,v) => v
    }
  }
}

case class Basic(val v: Locus) extends Var
case class Custom(val ty: Identifier, val params: List[Var], val v: Locus) extends Var
case class Product(val params: List[Var], val v: Locus) extends Var

class ConstructError(val msg: String) extends RuntimeException(s"Error: $msg")
case class UnknownIdentifier(val ty: String, val id: Identifier) extends ConstructError(s"Unkown $ty identifier, <${id.name}>")
case class TypeError(val v: Var, val expected: String) extends ConstructError(s"$v was expected to be a $expected, but is not!")
case class UsedIdentifier(val id: String) extends ConstructError(s"The identifier $id has already been used")

class ConstructInterpreter {

  val constructions = new HashMap[Identifier,Construction]
  val constructors = new HashMap[Identifier,Construction]
  var env = List[Item]()
  val vars = new HashMap[Identifier,Var]
  var internal_counter = 0
  val def_points = Queue(Point(0.0,0.0),Point(1.0,0.0),Point(1.0,1.0))
  private val builtins =
    List("circle", "line", "segment", "ray", "intersection") map {Identifier(_)}

  def checkFresh(id: Identifier) =
    if (vars.keys exists {_==id} ) throw new UsedIdentifier(id.name)

  def lookupPoint(id: Identifier) : Point = {
    val v = vars get id getOrElse {throw new UnknownIdentifier("point",id)}
    v.asPoint
  }

  def lookupConstructor(id: Identifier) : Construction =
    constructors get id getOrElse
      {throw new UnknownIdentifier("constructor",id)}

  def lookupConstruction(id: Identifier) : Construction =
    constructions get id getOrElse
      {throw new UnknownIdentifier("construction",id)}

  def lookupVar(id: Identifier) : Var =
    vars get id getOrElse {throw new UnknownIdentifier("",id)}

  def lookupLocus(id: Identifier) : Locus =
    lookupVar(id).asLocus

  def run(c: Construction, items: List[Item], in_vars: Option[List[Var]] = None): Var = {
    val Construction(_, params, statements, outs) = c
    inputs(params, in_vars)
    add_items(items)
    statements foreach {execute(_)}
    val vars = outs map {lookupVar(_)}
    if (vars.length > 1) {
      val loci = vars map {_.asLocus}
      val locus = loci.fold(Union(Set())){ _ union _ }
      Product(vars, locus)
    }
    else if (vars.length == 1) vars(0)
    else Basic(Union(Set()))
  }

  def add_items(items: List[Item]) = {
    env = items
    constructions ++= (items collect {case c: Construction => (c.name, c)})
    constructors  ++= (items collect {case Shape(c)        => (c.name, c)})
  }

  def inputs(params: List[Parameter], in_vars: Option[List[Var]]) = {
    in_vars match {
      case None => params foreach {case Parameter(id, ty) => {
        if (ty != Identifier("point"))
          throw new ConstructError(s"Implicit givens must be of type <point>, but ${id.name} is of type ${ty.name}")
        val pt = def_points.dequeue()
        vars += (id -> Basic(pt))
      }}
      case Some(ins_list) => {
        val assignments = params zip ins_list map {
          case (Parameter(name, ty), v) => {
            if (getTy(v) != ty) throw new ConstructError(s"Formal parameter has type $ty, but actual type was ${getTy(v)}.")
            (name, v)
          }
        }
        vars ++= assignments
      }
    }
  }

  def getTy(v: Var) : Identifier = {
    Identifier(v match {
      case Basic(_ : Line) => "line"
      case Basic(_ : Circle) => "circle"
      case Basic(_ : Segment) => "segment"
      case Basic(_ : Point) => "point"
      case Basic(_ : Ray) => "ray"
      case Basic(_ : Union) => "union"
      case _ : Product => throw new Error("types of products are unimplemented")
      case Custom(t, _, _) => t.name
    })
  }

  def intersection(v1: Var, v2: Var) : Var = {
    if (v1 == v2) throw new ConstructError("Cannot intersection a locus with itself")
    Basic(v1.asLocus intersect v2.asLocus)
  }

  def construct_line(p1: Var, p2: Var) : Var = {
    if (p1 == p2) throw new ConstructError("Cannot construct line from equal points")
    Basic(Line(p1.asPoint, p2.asPoint))
  }

  def construct_circle(c: Var, e: Var) : Var = {
    if (c == e) throw new ConstructError("Cannot construct circle from equal points")
    Basic(Circle(c.asPoint, e.asPoint))
  }

  def construct_ray(p1: Var, p2: Var) : Var = {
    if (p1 == p2) throw new ConstructError("Cannot construct ray from equal points")
    Basic(Ray(p1.asPoint, p2.asPoint))
  }

  def construct_segment(p1: Var, p2: Var) : Var = {
    if (p1 == p2) throw new ConstructError("Cannot construct segment from equal points")
    Basic(Segment(p1.asPoint, p2.asPoint))
  }

  def fn_call(fn: Identifier, ins: List[Var]) : Var = {
    if (constructors contains fn) constructor_call(fn, ins)
    else construction_call(fn, ins)
  }

  def procedure_call(fn: Identifier,
                     ins: List[Var],
                     lookup: Identifier => Construction) : Var = {
    val con = lookup(fn)
    val env_cons = env filter {_ != con}
    val con_in_count = con.parameters.length
    mk_arg_count_checker(con)(ins)
    val call_eval = new ConstructInterpreter
    call_eval.run(con, env_cons, Some(ins))
  }

  def constructor_call(fn: Identifier, ins: List[Var]) : Var =
    Custom(fn, ins, procedure_call(fn, ins, lookupConstructor(_)).asLocus)

  def construction_call(fn: Identifier, ins: List[Var]) : Var =
    procedure_call(fn, ins, lookupConstruction(_))

  def check_arg_count(fn: String, i: Int)(vars: List[Var]) =
    if (vars.length != i)
      throw new ConstructError(s"The construction <$fn> expects $i arguments, got ${vars.length}")

  def mk_arg_count_checker(c: Construction)(vars: List[Var]) = {
    val Construction(Identifier(name), params, _, _) = c
    check_arg_count(name, params.length)(vars)
  }

  def fn_evaluate(fn_app: FnApp) : Var = {
    val FnApp(fn, arg_exprs) = fn_app
    val arg_vars = arg_exprs map {evaluate(_)}
    val (arg0, arg1) = (arg_vars(0), arg_vars(1))
    fn match {
      case Identifier("intersection") => {
        check_arg_count("intersection", 2)(arg_vars)
        intersection(arg0, arg1)
      }
      case Identifier("circle") => {
        check_arg_count("circle", 2)(arg_vars)
        construct_circle(arg0, arg1)
      }
      case Identifier("line") => {
        check_arg_count("line", 2)(arg_vars)
        construct_line(arg0, arg1)
      }
      case Identifier("ray") => {
        check_arg_count("ray", 2)(arg_vars)
        construct_ray(arg0, arg1)
      }
      case Identifier("segment") => {
        check_arg_count("segment", 2)(arg_vars)
        construct_segment(arg0, arg1)
      }
      case fn_id => fn_call(fn_id, arg_vars)
    }
  }

  def evaluate(expr: Expr) : Var = {
    expr match {
      case fn_app: FnApp => fn_evaluate(fn_app)
      case Exactly(id) => lookupVar(id)
    }
  }

  def query(fn_id: Identifier) : Iterable[(List[Drawable],Expr,String)] = {
    val n_params = if (builtins contains fn_id) 2
                   else lookupConstruction(fn_id).parameters.length

    def try_call(fn_app: FnApp) : Option[Var] = {
      try { Some(fn_evaluate(fn_app)) }
      catch { case _ : ConstructError => None }
    }

    val possible_inputs = vars.keys map { Exactly(_) }
    val params =
      IterTools.cartesianProduct( ((1 to n_params) map { n => possible_inputs }).toList )
    val possible_calls = params map { FnApp(fn_id, _) }
    val results = possible_calls map { call => (try_call(call), call) }
    val filteredResults = filter_query_results(results)
    filteredResults.zipWithIndex map {
      case ((v, expr), i) => (make_named_split_set(i.toString, v), expr, i.toString)
    }
  }

  def filter_query_results(results: Iterable[(Option[Var],Expr)]) : Iterable[(Var,Expr)] = {
    val extantResults = results collect { case (Some(objs),call) => (objs, call) }
    val nonEmptyResults = extantResults filter {
      case (Basic(Union(loci)),_) => ! loci.isEmpty
      case _ => true
    }
    val newResults = nonEmptyResults filter {
      case (v, expr) => (vars.values count {v == _}) == 0
    }
    IterTools.uniqueBy(newResults, { x: (Var, Expr) => x._1 })
  }

  def execute(statement: Statement) = {
    val Statement(pattern, expr) = statement
    pattern_match(pattern, evaluate(expr))
  }

  def make_named(id: Identifier, v: Var) : Drawable = Drawable(id.name, v.asLocus)

  def make_named_split_set(id: String, v: Var) : List[Drawable] =
    v match {
      case Basic(Union(u)) => u.toList flatMap {l => make_named_split_set(id, Basic(l))}
      case Basic(locus) => List(Drawable(id, locus))
      case Custom(_, _, locus) => List(Drawable(id, locus))
      case Product(_, locus) => List(Drawable(id, locus))
    }

  def get_drawables : Iterable[Drawable] = vars map {case (id, v) => make_named(id, v)}

  def pattern_match(pattern: Pattern, v: Var) : Unit = {
    (pattern, v) match {
      case (Tuple(pats), Basic(Union(vars))) => {
        if (pats.length != vars.toList.length) {
          throw new ConstructError(s"Tried to bind the union $vars with ${vars.toList.length} items to the pattern ${Tuple(pats)} with ${pats.length} items")
        }
        pats zip (vars.toList map {Basic(_)}) map Function.tupled(pattern_match _ )
      }
      case (Id(id), v) => vars += (id -> v)
      case (Tuple(pats), Product(vars, _)) => {
        if (pats.length != vars.toList.length) {
          throw new ConstructError(s"Tried to bind the union $vars with ${vars.toList.length} items to the pattern ${Tuple(pats)} with ${pats.length} items")
        }
        pats zip vars.toList map Function.tupled(pattern_match _ )
      }
      case (Destructor(ty1, pats), Custom(ty2, vars, _)) => {
        if (ty1 != ty2) throw new TypeError(v, ty1.name)
        if (pats.length != vars.toList.length) {
          throw new ConstructError(s"Tried to bind the union $vars with ${vars.toList.length} items to the pattern ${Tuple(pats)} with ${pats.length} items")
        }
        pats zip vars.toList map Function.tupled(pattern_match _ )
      }
      case (pat, v) => { throw new ConstructError(s"Cannot match $v to pattern $pat") }
    }
  }

  override def toString : String = s"Variables: $vars"
}

object IterTools {
  def cartesianProduct[T](xss: List[Iterable[T]]): Iterable[List[T]] = xss match {
    case Nil => List(Nil)
    case h :: t => for(xh <- h; xt <- cartesianProduct(t)) yield xh :: xt
  }
  def uniqueBy[A, B](list: Iterable[A], fn: A => B) : List[A] = {
    val bs = scala.collection.mutable.MutableList[B]()
    val out = scala.collection.mutable.MutableList[A]()
    for (a <- list) {
      val f = fn(a)
      if (!(bs contains f)) {
        bs += f
        out += a
      }
    }
    return out.toList
  }
}
