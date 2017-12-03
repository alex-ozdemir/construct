// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the interpreter for Construct
package construct.semantics

import construct.engine._
import construct.input.ast._
import construct.output.Drawable

import scala.collection.mutable
import ConstructError._
import Value._
import construct.utils.IterTools

class ConstructInterpreter {

  val constructions = new mutable.HashMap[Identifier, Construction]
  val constructors = new mutable.HashMap[Identifier, Construction]
  val vars = new mutable.HashMap[Identifier, Value]
  val def_points =
    mutable.Queue(Point(0.0, 0.0),
                  Point(1.0, 0.0),
                  Point(1.7, 1.0),
                  Point(2.0, 0.5))

  def lookupConstructor(id: Identifier): Construction =
    constructors.getOrElse(id, throw UnknownIdentifier(id))

  def lookupConstruction(id: Identifier): Construction =
    constructions.getOrElse(id, throw UnknownIdentifier(id))

  def lookupVar(id: Identifier): Value =
    vars.getOrElse(id, throw UnknownIdentifier(id))

  def lookupLocus(id: Identifier): Locus =
    lookupVar(id).asLocus

  def run(c: Construction,
          items: Iterable[Item],
          in_vars: Option[List[Value]] = None): Value = {
    val Construction(_, params, statements, outs) = c
    set_inputs(params, in_vars)
    add_items(items)
    statements foreach execute
    outs map lookupVar match {
      case List()  => Product(List(), Union(Set()))
      case List(x) => x
      case xs =>
        Product(xs, xs map {
          _.asLocus
        } reduce {
          _ union _
        })
    }
  }

  def add_items(items: Iterable[Item]): Unit = {
    constructions ++= (items collect { case c: Construction => (c.name, c) })
    constructors ++= (items collect { case Shape(c)         => (c.name, c) })
  }

  def add_input(param: Parameter, actual_var: Option[Value]): Unit = {
    param match {
      case Parameter(id, ty) =>
        actual_var match {
          case None =>
            if (ty != Identifier("point"))
              throw ImplicitGiven(param)
            val pt = def_points.dequeue()
            vars += (id -> Basic(pt))
          case Some(v) =>
            if (getTy(v) != ty)
              throw ParameterTypeError(param, v)
            vars += ((id, v))
        }
    }
  }

  def set_inputs(params: Iterable[Parameter],
                 in_vars: Option[List[Value]]): Unit = {
    in_vars match {
      case None =>
        params foreach {
          add_input(_, None)
        }
      case Some(ins_list) =>
        params zip ins_list foreach {
          case (param, in) => add_input(param, Some(in))
        }
    }
  }

  def getTy(v: Value): Identifier = {
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

  def fn_call(fn: Identifier, ins: List[Value]): Value = {
    if (constructors contains fn) constructor_call(fn, ins)
    else construction_call(fn, ins)
  }

  def procedure_call(fn: Identifier,
                     ins: List[Value],
                     lookup: Identifier => Construction): Value = {
    val con = lookup(fn)
    val cons_in_new_env = constructions.values filter {
      _ != con
    }
    val shapes_in_new_env = constructors.values filter {
      _ != con
    } map {
      Shape
    }
    val env_cons = cons_in_new_env ++ shapes_in_new_env
    check_arg_count(con, ins)
    val call_eval = new ConstructInterpreter
    call_eval.run(con, env_cons, Some(ins))
  }

  def constructor_call(fn: Identifier, ins: List[Value]): Value =
    Custom(fn, ins, procedure_call(fn, ins, lookupConstructor).asLocus)

  def construction_call(fn: Identifier, ins: List[Value]): Value =
    procedure_call(fn, ins, lookupConstruction)

  def check_arg_count(c: Construction, vars: List[Value]): Unit =
    if (vars.length != c.parameters.length)
      throw Arity(c, vars)

  def fn_evaluate(fn_app: FnApp): Value = {
    val FnApp(fn_id, arg_exprs) = fn_app
    val arg_values = arg_exprs map {
      evaluate
    }
    Builtins.functionsMap.get(fn_id) map {
      _.execute(arg_values)
    } getOrElse {
      fn_call(fn_id, arg_values)
    }
  }

  def evaluate(expr: Expr): Value = {
    expr match {
      case fn_app: FnApp => fn_evaluate(fn_app)
      case Exactly(id)   => lookupVar(id)
      case SetLit(exprs) => ConstructInterpreter.mkValue(exprs map evaluate)
      case Difference(left, right) =>
        difference(evaluate(left), evaluate(right))
    }
  }

  def difference(left: Value, right: Value): Value = {
    (left, right) match {
      case (Product(left_vs, _), Product(right_vs, _)) =>
        ConstructInterpreter.mkValue(left_vs filter { v =>
          !(right_vs contains v)
        })
      case _ => throw InvalidDifference(left, right)
    }
  }

  def query(fn_id: Identifier): Iterable[(List[Drawable], Expr, String)] = {
    val n_params = Builtins.functionsMap get fn_id map {
      _.arity
    } getOrElse {
      lookupConstruction(fn_id).parameters.length
    }

    def try_call(fn_app: FnApp): Option[Value] = {
      try {
        Some(fn_evaluate(fn_app))
      } catch {
        case _: ConstructError => None
      }
    }

    val possible_inputs = vars.keys map Exactly
    val results = IterTools.cartesianProduct(
      Iterator.fill(n_params)(possible_inputs).toList) map {
      FnApp(fn_id, _)
    } map { call =>
      (try_call(call), call)
    }
    filter_query_results(results).zipWithIndex map {
      case ((v, expr), i) =>
        (make_named_split_set(i.toString, v), expr, i.toString)
    }
  }

  def filter_query_results(
      results: Iterable[(Option[Value], Expr)]): Iterable[(Value, Expr)] = {
    IterTools.uniqueBy(
      results collect { // Remove error results
        case (Some(objects), call) => (objects, call)
      } filter { // Remove empty Loci
        case (Product(_, locus), _) => locus.pointsIterator().nonEmpty
        case _                      => true
      } filter { // Remove old loci
        case (Product(_, Union(loci)), _) =>
          (loci.iterator count { s =>
            (vars.values count {
              Basic(s) == _
            }) == 0
          }) > 0
        case (Product(_, locus: PrimativeLocus), _) =>
          (vars.values count {
            Basic(locus) == _
          }) == 0
        case (v, _) =>
          (vars.values count {
            v == _
          }) == 0
      },
      { p: (Value, Expr) =>
        p._1
      }
    )
  }

  def execute(assignment: Statement): Traversable[Identifier] = {
    val Statement(pattern, expr) = assignment
    val boundIndents = pattern.boundIdents
    val reboundIdents = boundIndents intersect vars.keySet
    if (reboundIdents.nonEmpty)
      throw Rebind(pattern, reboundIdents.iterator.next())
    pattern_match(pattern, evaluate(expr))
    boundIndents
  }

  def make_named(id: Identifier, v: Value): Drawable =
    Drawable(id.name, v.asLocus)

  def make_named_split_set(id: String, v: Value): List[Drawable] =
    v match {
      case Basic(locus)        => List(Drawable(id, locus))
      case Custom(_, _, locus) => List(Drawable(id, locus))
      case Product(_, Union(s)) =>
        s.toList map Basic flatMap { l =>
          make_named_split_set(id, l)
        }
      case Product(_, locus) => List(Drawable(id, locus))
    }

  def get_drawables: Iterable[Drawable] =
    vars map Function.tupled(make_named)

  def pattern_match(pattern: Pattern, value: Value): Unit = {
    (pattern, value) match {
      case (Id(id), v) => vars += (id -> v)
      case (u @ Tuple(pats), prod @ Product(values, _)) =>
        if (pats.length != values.length)
          throw ProductBindError(u, prod)
        pats zip values foreach Function.tupled(pattern_match)
      case (Destructor(id, pats), v) if Builtins.types.exists(_.id == id) =>
        Builtins
          .typesMap(id)
          .bind(pats, v)
          .fold(
            {
              case InternalError.BuiltinBindArityError(b, i) =>
                throw ConstructError.BuiltinBindArityError(b, i, pattern)
              case InternalError.BuiltinBindValueError(b, _, va) =>
                throw ConstructError.BuiltinBindValueError(b, va, pattern)
            }, {
              _ foreach Function.tupled(pattern_match)
            }
          )
      case (s @ Destructor(ty1, pats), c @ Custom(ty2, values, _)) =>
        if (ty1 != ty2) throw DestructorBindTypeError(s, c)
        if (pats.length != values.length) throw DestructorBindArityError(s, c)
        pats zip values foreach Function.tupled(pattern_match)
      case (pat, v) =>
        throw VagueBindError(pat, v)
    }
  }

  override def toString: String = {
    "Variables:\n" +
      (vars map { case (k, v) => s"  ${k.name} => $v" } mkString "\n") +
      "\nConstructions:\n" +
      (constructions map { case (k, _) => s"  ${k.name} => ..." } mkString "\n") +
      "\nConstructors:\n" +
      (constructors map { case (k, _) => s"  ${k.name} => ..." } mkString "\n")
  }
}

object ConstructInterpreter {
  def mkValue(params: List[Value]): Value = {
    params match {
      case List(p) => p
      case ps =>
        Product(ps,
                ps.map {
                    _.asLocus
                  }
                  .foldLeft(Union(Set()): Locus) {
                    _ union _
                  })

    }
  }
}
