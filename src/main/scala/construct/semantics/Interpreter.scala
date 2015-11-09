package construct.semantics

import construct.engine._
import construct.input.ast._
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap

class ConstructError(val msg: String) extends RuntimeException(msg)
class UnknownIdentifier(override val msg: String) extends ConstructError(msg)

class ConstructInterpreter {
  val cons = new HashMap[Identifier,Construction]
  var env = List[Construction]()
  val points = new HashMap[Identifier,Point]
  val loci = new HashMap[Identifier,PrimativeLocus]
  val objects = new HashMap[Identifier,NamedObject]
  var internal_counter = 0
  val def_points = Queue(Point(0.0,0.0),Point(1.0,0.0),Point(1.0,1.0))

  def lookupPoint(id: Identifier) : Point =
    points get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}

  def lookupConstruction(id: Identifier) : Construction =
    cons get id getOrElse {throw new UnknownIdentifier(s"Unknown construction identifier $id")}

  def lookupNamedPoint(id: Identifier) : NamedPoint =
    objects get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}
      match {
        case p : NamedPoint => p
        case _ => throw new UnknownIdentifier(s"The identifier $id is not a point")
      }


  def lookupLocus(id: Identifier) : PrimativeLocus =
    loci get id getOrElse {throw new UnknownIdentifier(s"Unknown locus identifier $id")}

  def nextInternalId() : Identifier = {
    internal_counter += 1
    Identifier(s"::Item#${internal_counter}")
  }

  def run(c: Construction, cs: List[Construction], in_pts: Option[List[Point]] = None): List[Point] = {
    val Construction(_, ins, statements, outs) = c
    inputs(ins, in_pts)
    constructions(cs)
    statements foreach {execute(_)}
    outs map {lookupPoint(_)}
  }

  def constructions(cs: List[Construction]) = {
    env = cs
    cons ++= (cs map {c => (c.name, c)})
  }

  def inputs(params: List[Identifier], ins: Option[List[Point]]) = {
    ins match {
      case None => params foreach {id => {
        val pt = def_points.dequeue()
        points += (id -> pt)
        objects += (id -> NamedPoint(id.name, pt))
      }}
      case Some(ins_list) => {
        points ++= params zip ins_list
        objects ++= params zip ins_list map {case (id, pt) => (id, NamedPoint(id.name, pt))}
      }
    }
  }

  def intersection(inter_ids: List[Identifier], id1: Identifier, id2: Identifier) = {
    val locus1 = lookupLocus(id1)
    val locus2 = lookupLocus(id2)
    val interLocus = locus1 intersect locus2
    if (!interLocus.isPoints) {
      throw new ConstructError("Intersection calls must result in only points!" +
        s"Found lines ${interLocus.asLines} and circles ${interLocus.asCircles}.");
    }
    val inters = interLocus.asPoints
    require(inters.size == inter_ids.size)
    points ++= inter_ids zip inters
    objects ++= inter_ids zip inters map {case (id, pt) => (id, NamedPoint(id.name, pt))}
  }

  def construct_line(line_id: Identifier, id1: Identifier, id2: Identifier) = {
    val pt1 = lookupPoint(id1)
    val pt2 = lookupPoint(id2)
    val named_pt1 = lookupNamedPoint(id1)
    val named_pt2 = lookupNamedPoint(id2)
    loci += (line_id -> Line(pt1, pt2))
    objects += (line_id -> NamedLine(line_id.name, named_pt1, named_pt2))
  }

  def construct_circle(circ_id: Identifier, c_id: Identifier, e_id: Identifier) = {
    val c = lookupPoint(c_id)
    val e = lookupPoint(e_id)
    val named_c = lookupNamedPoint(c_id)
    val named_e = lookupNamedPoint(e_id)
    loci += (circ_id -> Circle(c, e))
    objects += (circ_id -> NamedCircle(circ_id.name, named_c, named_e))
  }

  def fn_call(fn: Identifier, outs: List[Identifier], ins: List[Identifier]) = {
    val con = lookupConstruction(fn)
    val env_cons = env filter {_ != con}
    val in_pts = ins map {lookupPoint(_)}
    val con_in_count = con.parameters.length
    val con_out_count = con.returns.length
    if (con_in_count != ins.length)
      throw new ConstructError(s"Calling construction $fn with ${ins.length} parameters instead of" +
        s" the require $con_in_count.")
    if (con_out_count != outs.length)
      throw new ConstructError(s"Calling construction $fn expecting ${outs.length} outputs instead" +
        s" of the resulting $con_out_count.")
    val call_eval = new ConstructInterpreter
    val out_pts = call_eval.run(con, env_cons, Some(in_pts))
    points ++= outs zip out_pts
    objects ++= outs zip out_pts map {case (id, pt) => (id, NamedPoint(id.name, pt))}
  }

  def execute(statement: Statement) = {
    statement match {
      case Statement(outs, Identifier("intersection"), ins) => {
        if (ins.length != 2) { throw new ConstructError("Can only take the intersection of 2 loci") }
        else {
          val List(id1, id2) = ins
          intersection(outs, id1, id2)
        }
      }
      case Statement(out, Identifier("circle"), ins) => {
        println(s"Constructing a circle from: $ins")
        if (ins.length != 2) { throw new ConstructError("Can only construct a circle from 2 points") }
        else if (out.length != 1) { throw new ConstructError("Constructing a circle results in 1 locus") }
        else {
          val List(center, edge) = ins
          val List(circ_id) = out
          construct_circle(circ_id, center, edge)
        }
      }
      case Statement(out, Identifier("line"), ins) => {
        if (ins.length != 2) { throw new ConstructError("Can only construct a circle form 2 points") }
        else if (out.length != 1) { throw new ConstructError("Constructing a circle results in 1 locus") }
        else {
          val List(center, edge) = ins
          val List(circ_id) = out
          construct_line(circ_id, center, edge)
        }
      }
      case Statement(outs, fn, ins) => { fn_call(fn, outs, ins) }
    }
  }

  override def toString : String = s"Points: $points \nLoci: $loci"
}
