package construct.semantics

import construct.engine._
import construct.input.ast._
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
    }
  }
}

case class Basic(val v: Locus) extends Var
// case class Custom(val ty: Identifier, val params: List[Var], val v: Locus) extends Var

class ConstructError(val msg: String) extends RuntimeException(s"Error: $msg")
class UnknownIdentifier(override val msg: String) extends ConstructError(msg)
class TypeError(val v: Var, val expected: String) extends ConstructError(s"$v was expected to be a $expected, but is not!")
class UsedIdentifier(val id: String) extends ConstructError(s"The identifier $id has already been used")

class ConstructInterpreter {
  private val POINT = "point"
  private val LINE  = "line"
  private val CIRCLE  = "circle"
  private val UNION  = "union"

  val cons = new HashMap[Identifier,Construction]
  var env = List[Construction]()
  val vars = new HashMap[Identifier,Var]
  val objects = new HashMap[Identifier,NamedObject]
  var internal_counter = 0
  val def_points = Queue(Point(0.0,0.0),Point(1.0,0.0),Point(1.0,1.0))

  def checkFresh(id: Identifier) =
    if (vars.keys exists {_==id} ) throw new UsedIdentifier(id.name)

  def lookupPoint(id: Identifier) : Point = {
    val v = vars get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}
    v.asPoint
  }

  def lookupConstruction(id: Identifier) : Construction =
    cons get id getOrElse {throw new UnknownIdentifier(s"Unknown construction identifier $id")}

  def lookupNamedPoint(id: Identifier) : NamedPoint =
    objects get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}
      match {
        case p : NamedPoint => p
        case _ => throw new UnknownIdentifier(s"The identifier $id is not a point")
      }


  def lookupVar(id: Identifier) : Var =
    vars get id getOrElse {throw new UnknownIdentifier(s"Unknown identifier $id")}

  def lookupLocus(id: Identifier) : Locus =
    lookupVar(id) match {
      case Basic(locus) => locus
      // TODO: Handle cutom
    }

  def nextInternalId() : Identifier = {
    internal_counter += 1
    Identifier(s"TmpItem${internal_counter}")
  }

  def run(c: Construction, cs: List[Construction], in_vars: Option[List[Var]] = None): Var = {
    val Construction(_, in_ids, statements, outs) = c
    inputs(in_ids, in_vars)
    constructions(cs)
    statements foreach {execute(_)}
    val loci = outs map {lookupVar(_)} map {case Basic(locus) => locus} 
    Basic(loci.fold(Union(Set())){ _ union _ })
  }

  def constructions(cs: List[Construction]) = {
    env = cs
    cons ++= (cs map {c => (c.name, c)})
  }

  def inputs(in_ids: List[Identifier], in_vars: Option[List[Var]]) = {
    in_vars match {
      case None => in_ids foreach {id => {
        val pt = def_points.dequeue()
        vars += (id -> Basic(pt))
        register_names(id, Basic(pt))
      }}
      case Some(ins_list) => {
        vars ++= in_ids zip ins_list
        in_ids zip ins_list map { case (id, v) => register_names(id, v) }
      }
    }
  }

  def intersection(v1: Var, v2: Var) : Var =
    Basic(v1.asLocus intersect v2.asLocus)

//     if (!interLocus.isPoints) {
//       throw new ConstructError("Intersection calls must result in only points!" +
//         s"Found lines ${interLocus.asLines} and circles ${interLocus.asCircles}.");
//     }
//     val inters = interLocus.asPoints
//     require(inters.size == inter_ids.size)
//     points ++= inter_ids zip inters
//     objects ++= inter_ids zip inters map {case (id, pt) => (id, NamedPoint(id.name, pt))}

  def construct_line(p1: Var, p2: Var) : Var =
    Basic(Line(p1.asPoint, p2.asPoint))
//     val pt1 = lookupPoint(id1)
//     val pt2 = lookupPoint(id2)
//     val named_pt1 = lookupNamedPoint(id1)
//     val named_pt2 = lookupNamedPoint(id2)
//     loci += (line_id -> Line(pt1, pt2))
//     objects += (line_id -> NamedLine(line_id.name, named_pt1, named_pt2))

  def construct_circle(c: Var, e: Var) : Var =
    Basic(Circle(c.asPoint, e.asPoint))

//     checkFresh(circ_id)
//     val c = lookupPoint(c_id)
//     val e = lookupPoint(e_id)
//     val named_c = lookupNamedPoint(c_id)
//     val named_e = lookupNamedPoint(e_id)
//     loci += (circ_id -> Circle(c, e))
//     objects += (circ_id -> NamedCircle(circ_id.name, named_c, named_e))

  def fn_call(fn: Identifier, ins: List[Var]) : Var = {
    val con = lookupConstruction(fn)
    val env_cons = env filter {_ != con}
    val con_in_count = con.parameters.length
    mk_arg_count_checker(con)(ins)
    val call_eval = new ConstructInterpreter
    call_eval.run(con, env_cons, Some(ins))
  }

  def check_builtin_arg_count(fn: String, i: Int)(vars: List[Var]) =
    if (vars.length != i)
      throw new ConstructError(s"The builtin construction <$fn> expects $i arguments, got ${vars.length}")

  def check_arg_count(fn: String, i: Int)(vars: List[Var]) =
    if (vars.length != i)
      throw new ConstructError(s"The construction <$fn> expects $i arguments, got ${vars.length}")

  def mk_arg_count_checker(c: Construction)(vars: List[Var]) = {
    val Construction(Identifier(name), params, _, _) = c
    check_arg_count(name, params.length)(vars)
  }

  def evaluate(expr: Expr) : Var = {
    expr match {
      case FnApp(fn, arg_exprs) => {
        val arg_vars = arg_exprs map {evaluate(_)}
        fn match {
          case Identifier("intersection") => {
            check_arg_count("intersection", 2)(arg_vars)
            intersection(arg_vars(0), arg_vars(1))
          }
          case Identifier("circle") => {
            check_arg_count("circle", 2)(arg_vars)
            construct_circle(arg_vars(0), arg_vars(1))
          }
          case Identifier("line") => {
            check_arg_count("line", 2)(arg_vars)
            construct_line(arg_vars(0), arg_vars(1))
          }
          case fn_id => {
            fn_call(fn_id, arg_vars)
          }
        }
      }
      case Exactly(id) => lookupVar(id)
    }
  }

  def execute(statement: Statement) = {
    val Statement(pattern, expr) = statement
    pattern_match(pattern, evaluate(expr))
  }

  def register_names(id: Identifier, v: Var) : Unit = {
    v match {
      case Basic(pt : Point) => {
        val npt = NamedPoint(id.name, pt)
        objects += (id -> npt)
      }
      case Basic(Line(pt1, pt2)) => {
        val pt1_id = nextInternalId
        val pt2_id = nextInternalId
        register_names(pt1_id, Basic(pt1))
        register_names(pt2_id, Basic(pt2))
        val named_line = NamedLine(id.name,
                                   NamedPoint(pt1_id.name, pt1),
                                   NamedPoint(pt2_id.name, pt2))
        objects += (id -> named_line)
      }
      case Basic(Circle(pt1, pt2)) => {
        val pt1_id = nextInternalId
        val pt2_id = nextInternalId
        register_names(pt1_id, Basic(pt1))
        register_names(pt2_id, Basic(pt2))
        val named_circle = NamedCircle(id.name,
                                      NamedPoint(pt1_id.name, pt1),
                                      NamedPoint(pt2_id.name, pt2))
        objects += (id -> named_circle)
      }
      case x => throw new Error(s"Could not create visual for $x")
    }
  }

  def pattern_match(pattern: Pattern, v: Var) : Unit = {
    (pattern, v) match {
      case (Tuple(pats), Basic(Union(vars))) => {
        if (pats.length != vars.toList.length) {
          throw new ConstructError(s"Tried to bind the union $vars with ${vars.toList.length} item to the pattern ${Tuple(pats)} with ${pats.length} item")
        }
        pats zip (vars.toList map {Basic(_)}) map Function.tupled(pattern_match _ )
      }
      case (Id(id), v) => {
        vars += (id -> v)
        register_names(id, v)
      }
      case _ => { throw new Error("unimplemented") }
      // TODO Custom & Desctructor
    }
  }

  override def toString : String = s"Variables: $vars"
}
