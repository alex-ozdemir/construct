package construct.semantics

import construct.engine._
import construct.input.ast._
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap

class ConstructError(val msg: String) extends RuntimeException
class UnknownIdentifier(override val msg: String) extends ConstructError(msg)

case class NamedPoint(val name: String, val point: Point)

class ConstructInterpreter {
  val points = new HashMap[Identifier,Point]
  val loci = new HashMap[Identifier,PrimativeLocus]
  var internal_counter = 0
  val def_points = Queue(Point(0.0,0.0),Point(1.0,0.0))

  def lookupPoint(id: Identifier) : Point =
    points get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}

  def lookupLocus(id: Identifier) : PrimativeLocus =
    loci get id getOrElse {throw new UnknownIdentifier(s"Unknown locus identifier $id")}

  def nextInternalId() : Identifier = {
    internal_counter += 1
    Identifier(s"::Item#${internal_counter}")
  }

  def apply(statement: Statement) = {
    statement match {
      case LetStatement(id, constructor) => construct(id, constructor)
      case constructor: Constructor => construct(nextInternalId(), constructor)
      case IntersectionStatement(id1, id2, inter_ids) => {
        val locus1 = lookupLocus(id1)
        val locus2 = lookupLocus(id2)
        val inters = locus1 intersect locus2
        // require(inters.size == inter_ids.size)
        // points ++= inter_ids zip inters
      }
    }
  }

  def construct(id: Identifier, constructor: Constructor) = {
    constructor match {
      case PointConstructor() => points += (id -> def_points.dequeue())
      case LineConstructor(pt_id1, pt_id2) => {
        val pt1 = lookupPoint(pt_id1)
        val pt2 = lookupPoint(pt_id2)
        loci += (id -> Line(pt1, pt2))
      }
      case CircleConstructor(pt_id1, pt_id2) => {
        val pt1 = lookupPoint(pt_id1)
        val pt2 = lookupPoint(pt_id2)
        loci += (id -> Circle(pt1, pt2))
      }
    }
  }

  def out : String = s"Points: $points \nLoci: $loci"
}
