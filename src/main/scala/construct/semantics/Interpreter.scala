package construct.semantics

import construct.engine._
import construct.input.ast._
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap

class ConstructError(val msg: String) extends RuntimeException
class UnknownIdentifier(override val msg: String) extends ConstructError(msg)

class ConstructInterpreter {
  val points = new HashMap[Identifier,Point]
  val loci = new HashMap[Identifier,PrimativeLocus]
  val objects = new HashMap[Identifier,NamedObject]
  var internal_counter = 0
  val def_points = Queue(Point(0.0,0.0),Point(1.0,0.0),Point(1.0,1.0))

  def lookupPoint(id: Identifier) : Point =
    points get id getOrElse {throw new UnknownIdentifier(s"Unknown point identifier $id")}

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

  def inputs(parameters: Parameters) = {
    parameters.parameters foreach {id => {
      val pt = def_points.dequeue()
      points += (id -> pt)
      objects += (id -> NamedPoint(id.name, pt))
    }}
  }

  def apply(statement: Statement) = {
    statement match {
      case LetStatement(id, constructor) => construct(id, constructor)
      case constructor: Constructor => construct(nextInternalId(), constructor)
      case PointsIntersectionStatement(id1, id2, inter_ids) => {
        val locus1 = lookupLocus(id1)
        val locus2 = lookupLocus(id2)
        val interLocus = locus1 intersect locus2
        if (!interLocus.isPoints) {
          throw new ConstructError("Point intersection statements must result in only points!" +
            s"Found lines ${interLocus.asLines} and circles ${interLocus.asCircles}.");
        }
        val inters = interLocus.asPoints
        require(inters.size == inter_ids.size)
        points ++= inter_ids zip inters
        objects ++= inter_ids zip inters map {case (id, pt) => (id, NamedPoint(id.name, pt))}
      }
    }
  }

  def construct(id: Identifier, constructor: Constructor) = {
    constructor match {
      case PointConstructor() => points += (id -> def_points.dequeue())
      case LineConstructor(pt_id1, pt_id2) => {
        val pt1 = lookupPoint(pt_id1)
        val pt2 = lookupPoint(pt_id2)
        val named_pt1 = lookupNamedPoint(pt_id1)
        val named_pt2 = lookupNamedPoint(pt_id2)
        loci += (id -> Line(pt1, pt2))
        objects += (id -> NamedLine(id.name, named_pt1, named_pt2))
      }
      case CircleConstructor(pt_id1, pt_id2) => {
        val pt1 = lookupPoint(pt_id1)
        val pt2 = lookupPoint(pt_id2)
        val named_pt1 = lookupNamedPoint(pt_id1)
        val named_pt2 = lookupNamedPoint(pt_id2)
        loci += (id -> Circle(pt1, pt2))
        objects += (id -> NamedCircle(id.name, named_pt1, named_pt2))
      }
    }
  }



  override def toString : String = s"Points: $points \nLoci: $loci"
}
