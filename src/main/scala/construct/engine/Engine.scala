// Alex Ozdemir <aozdemir@hmc.edu>
// December 2015
//
// This file holds the geometry engine for the Construct language
//
// The essential type is a Locus, which can take multiple forms
// The concrete ones are in [square brackets]
//
// Locus
//    SingleLocus
//       PrimitiveLocus
//          [Line]
//          [Ray]
//          [Segment]
//          [Circle]
//       [Point]
//    [Union]

package construct.engine;

import scala.math;

object Closeness {
  val EPSILON = 0.000001
  implicit class CloseDouble(x: Double) {
    def ===(that: Double) : Boolean = math.abs(x - that) < EPSILON
  }
}

sealed abstract class Locus {
  def asPoints : List[Point] = List()
  def asLines : List[Line] = List()
  def asCircles : List[Circle] = List()
  def isPoints : Boolean = asLines == List() && asCircles == List() && asPoints != List()
  def intersect(other: Locus) : Locus
  def union(other: Locus) : Locus
  def choose : Option[Point]
  def contains(that: Point) : Boolean
  def empty : Boolean = choose.isEmpty
}

sealed abstract class SingleLocus extends Locus {
  def intersect(other: Locus) : Locus
  def union(other: Locus) : Locus = {
    if (this == other) this
    else other match {
      case single: SingleLocus => Union(Set(this, single))
      case union: Union => Union(union.set + this)
    }
  }
  def contains(that: Point) : Boolean
  def choose : Option[Point]
}

sealed abstract class PrimativeLocus extends SingleLocus {
  def intersect(that: Locus) : Locus =
    that match {
      case p: Point => if (this contains p) p else Union(Set())
      case p: PrimativeLocus => this intersectPrim p
      case u: Union => u intersect this
    }
  def intersectPrim(that: PrimativeLocus) : Locus = Intersection.intersect(this, that)
  def contains(that: Point) : Boolean
  def choose : Option[Point]
}

case class Union(val set: Set[SingleLocus]) extends Locus {

  override def asPoints : List[Point] = (set collect {case x: Point => x}).toList
  override def asLines : List[Line] = (set collect {case x: Line => x}).toList
  override def asCircles : List[Circle] = (set collect {case x: Circle => x}).toList

  override def equals(that: Any) : Boolean =
    that match {
      case union: Union => set == union.set
      case x => if (set.size == 1) x == set.toList(0) else false
    }

  def intersect(that: Locus) : Locus = set map { _ intersect that } reduce { _ union _ }

  def union(other: Locus) : Locus =
    if (this == other) this
    else if (this == Union(Set())) other
    else other match {
      case single: SingleLocus => Union(set + single)
      case union: Union => Union(union.set ++ set)
    }

  def choose : Option[Point] = if (set.isEmpty) None else set.head.choose

  def contains(that: Point) = set exists { _ contains that }

  override def empty : Boolean = set.isEmpty
}

case class Point(val x: Double, val y: Double) extends SingleLocus {
  import Closeness._
  override def equals(other: Any) : Boolean = {
    other match {
      case p: Point => math.sqrt( (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y)) === 0
      case _ => false
    }
  }
  def intersect(other: Locus) : Locus = if (other contains this) this else Union(Set())
  def choose : Option[Point] = Some(this)
  override def asPoints : List[Point] = List(this)
  def +(that: Point) : Point = Point(this.x + that.x, this.y + that.y)
  def -(that: Point) : Point = Point(this.x - that.x, this.y - that.y)
  def *(that: Double) : Point = Point(this.x * that, this.y * that)
  def /(that: Double) : Point = Point(this.x / that, this.y / that)
  def /(that: Point) : Option[Double] =
    if ((!(this reject that)) === 0)
      Some(if (x == 0) y / that.y
           else x / that.x)
    else None
  def <>(that: Point) : Double = x * that.x + y * that.y
  def project(that: Point) : Point = that * ((this <> that) / math.pow(!that, 2))
  def reject(that: Point) : Point = this - (this project that)
  def rotate(ang: Double) : Point = {
    val s = math.sin(ang)
    val c = math.cos(ang)
    Point(x * c - y * s, y * c + x * s)
  }
  def withMag(mag: Double) = this * (mag / !this)
  def unary_!() : Double = math.sqrt(x * x + y * y)
  def angle : Double = math.atan2(y, x)
  def contains(that: Point) : Boolean = this == that
}

case class Circle(val c: Point, val p: Point) extends PrimativeLocus {
  import Closeness._
  val r_sq = (c.y - p.y) * (c.y - p.y) + (c.x - p.x) * (c.x - p.x)
  val r = math.sqrt(r_sq)
  def param(pt: Point) : Option[Double] = {
    if (this contains pt)
      Some((pt - c).angle)
    else
      None
  }

  override def equals(other: Any) : Boolean = {
    other match {
      case circle: Circle => c == circle.c && r == circle.r
      case _ => false
    }
  }

  def contains(pt: Point) : Boolean = !(pt - c) === r

  def choose : Option[Point] = Some(p)
  override def asCircles : List[Circle] = List(this)
}

case class Line(val p1: Point, val p2: Point) extends PrimativeLocus {
  import Closeness._
  val angle = math.atan2(p2.y - p1.y, p2.x - p1.x)
  val standard_form = if (p1.x === p2.x) None
                      else {
                        val slope = (p2.y - p1.y) / (p2.x - p1.x)
                        Some(slope, p1.y - slope * p1.x)
                      }

  override def equals(other: Any) : Boolean = {
    other match {
      case line: Line => {
        (standard_form, line.standard_form) match {
          case (Some((s, y)), Some((s_other, y_other))) => s == s_other && y == y_other
          case (None, None) => line.p1.x == p1.x
          case _ => false
        }
      }
      case _ => false
    }
  }

  def contains(pt: Point) : Boolean = !((pt - p1) reject (p2 - p1)) === 0

  def choose : Option[Point] = Some(p2)
  override def asLines : List[Line] = List(this)
}

case class Ray(val p1: Point, val p2: Point) extends PrimativeLocus {
  import Closeness._
  val angle = math.atan2(p2.y - p1.y, p2.x - p1.x)

  def asLine: Line = Line(p1,p2)

  def param(pt: Point) : Option[Double] = (pt - p1) / (p2 - p1)

  override def equals(other: Any) : Boolean = {
    other match {
      case ray: Ray => {
        return (p1 == ray.p1) && (angle === ray.angle)
      }
      case _ => false
    }
  }

  def contains(pt: Point) : Boolean =
    (!((pt - p1) reject (p2 - p1))) === 0 && param(pt).get >= 0

  def choose : Option[Point] = Some(p2)
}

case class Segment(val p1: Point, val p2: Point) extends PrimativeLocus {
  import Closeness._
  val angle = math.atan2(p2.y - p1.y, p2.x - p1.x)

  def asLine: Line = Line(p1,p2)

  def param(pt: Point) : Option[Double] = (pt - p1) / (p2 - p1)

  override def equals(other: Any) : Boolean = {
    other match {
      case that: Segment => {
        return (this.p1 == that.p1 && this.p2 == that.p2) ||
               (this.p1 == that.p2 && this.p2 == that.p1)
      }
      case _ => false
    }
  }

  def contains(pt: Point) : Boolean = !((pt - p1) reject (p2 - p1)) === 0 && param(pt).get >= 0 && param(pt).get <= 1

  def choose : Option[Point] = Some(p2)
}


// Holds all the intersectioni logic for primitives
object Intersection {
  import Closeness._
  def intersect(A: PrimativeLocus, B: PrimativeLocus) : Locus =
    (A, B) match {
      case (a: Circle , b: Circle) => CircleCircle(a, b)
      case (a: Circle , b: Line) => CircleLine(a, b)
      case (a: Circle , b: Ray) => CircleRay(a, b)
      case (a: Circle , b: Segment) => CircleSegment(a, b)
      case (a: Line , b: Circle) => LineCircle(a, b)
      case (a: Line , b: Line) => LineLine(a, b)
      case (a: Line , b: Ray) => LineRay(a, b)
      case (a: Line , b: Segment) => LineSegment(a, b)
      case (a: Ray , b: Circle) => RayCircle(a, b)
      case (a: Ray , b: Line) => RayLine(a, b)
      case (a: Ray , b: Ray) => RayRay(a, b)
      case (a: Ray , b: Segment) => RaySegment(a, b)
      case (a: Segment , b: Circle) => SegmentCircle(a, b)
      case (a: Segment , b: Line) => SegmentLine(a, b)
      case (a: Segment , b: Ray) => SegmentRay(a, b)
      case (a: Segment , b: Segment) => SegmentSegment(a, b)
    }
  def CircleCircle(A: Circle, B: Circle) : Locus = {
    if (A == B) A
    else {
      val dist = !(A.c - B.c)
      if (A.r_sq < B.r_sq) intersect(B,A)
      else {
        val r_towards_B = (B.c - A.c) * A.r / dist
        if (A.r + B.r === dist || B.r + dist === A.r)
          A.c + r_towards_B
        else if (A.r + B.r < dist || B.r + dist < A.r)
          Union(Set())
        else {
          val ang = math.acos((A.r_sq + dist * dist - B.r_sq) / (2 * A.r * dist))
          Union(Set(A.c + r_towards_B.rotate(ang), A.c + r_towards_B.rotate(-ang)))
        }
      }
    }
  }
  def CircleLine(A: Circle, B: Line) : Locus = intersect(B, A)
  def LineCircle(D: Line, E: Circle) : Locus = {
    D.standard_form match {
      case Some((m, b)) => {
        // Line  : y = m * x + b
        // Circle: (y - c.y)^2 + (x - c.x)^2 = r^2
        //
        // Solution: Find x in
        // Ax^2 + Bx + C = 0
        val A = math.pow(m,2) + 1
        val B = 2 * (b - E.c.y) * m - 2 * E.c.x
        val C = math.pow(b - E.c.y, 2) + math.pow(E.c.x, 2) - E.r_sq
        val desc = math.pow(B,2) - 4 * A * C;
        if (desc === 0) {
          val x = -B / (2 * A);
          Point(x, m * x + b)
        }
        else if (desc < 0) {
          Union(Set())
        }
        else {
          val x1 = (-B - math.sqrt(desc)) / (2 * A)
          val x2 = (-B + math.sqrt(desc)) / (2 * A)
          Union(Set(Point(x1, m * x1 + b), Point(x2, m * x2 + b)))
        }
      }
      case None => {
        val line_x = D.p1.x
        if (line_x === E.c.x - E.r || line_x === E.c.x + E.r) {
          Point(line_x, E.c.y)
        } else if (math.abs(line_x - E.c.x) < E.r) {
          val y_diff = math.sqrt(E.r_sq - math.pow(line_x - E.c.x,2))
          Union(Set(Point(line_x,E.c.y + y_diff), Point(line_x, E.c.y - y_diff)))
        } else {
          Union(Set())
        }
      }
    }
  }
  def LineLine(A: Line, B: Line) : Locus = {
    (A.standard_form, B.standard_form) match {
      case (Some((s, y)), Some((bs, by))) => {
        if (s === bs)
          if (y === by)
            A
          else
            Union(Set())
        else {
          val x = (y - by) / (bs - s)
          Point(x, y + s * x)
        }
      }
      case (None, Some((_,_))) => intersect(B, A)
      case (Some((slope, y0)),None) => Point(B.p1.x, y0 + B.p1.x * slope)
      case (None, None) => {
        if (A.p1.x === B.p1.x)
          A
        else
          Union(Set())
      }
    }
  }
  def RayRay(A: Ray, B: Ray) : Locus = {
    if (A.angle === B.angle) {
      (B contains A.p1, A contains B.p1) match {
        case (true, true) => A
        case (true, false) => A
        case (false, true) => B
        case (false, false) => Union(Set())
      }
    }
    else if ((A.angle + math.Pi) % math.Pi === (B.angle + math.Pi) % math.Pi) {
      if (B.p1 == A.p1) A.p1
      else if (A contains B.p1) Segment(B.p1, A.p1)
      else Union(Set())
    }
    else {
      val List(p) = intersect(A.asLine, B.asLine).asPoints
      if ((A contains p) && (B contains p)) p
      else Union(Set())
    }
  }
  def LineRay(A: Line, B: Ray) : Locus = intersect(B, A)
  def RayLine(A: Ray, B: Line) : Locus = {
    if ((A.angle + math.Pi) % math.Pi === (B.angle + math.Pi) % math.Pi) {
      if (B contains A.p1) A
      else Union(Set())
    }
    else {
      val List(p) = intersect(A.asLine, B).asPoints
      if (A contains p) p
      else Union(Set())
    }
  }
  def CircleRay(A: Circle, B: Ray) : Locus = intersect(B, A)
  def RayCircle(A: Ray, B: Circle) : Locus = {
    intersect(A.asLine, B) match {
      case Union(ps) => {
        Union(ps filter {
          case p: Point => A contains p
          case _ => false
        })
      }
      case p: Point => if (A contains p) p else Union(Set())
      case _ => throw new Error("RayCircle bug")
    }
  }
  def SegmentSegment(A: Segment, B: Segment) : Locus = {
    if ((A.angle + math.Pi) % math.Pi === (B.angle + math.Pi) % math.Pi) {
      (B contains A.p1, B contains A.p2) match {
        case (true, true) => A
        case (true, false) => {
          if (A contains B.p1) Segment(A.p1, B.p1)
          else Segment(A.p1, B.p2)
        }
        case (false, true) => {
          if (A contains B.p1) Segment(A.p2, B.p1)
          else Segment(A.p2, B.p2)
        }
        case (false, false) => {
          if ((A contains B.p1) && (A contains B.p2)) B
          else Union(Set())
        }
      }
    }
    else {
      val List(p) = intersect(A.asLine, B.asLine).asPoints
      if ((A contains p) && (B contains p)) p
      else Union(Set())
    }
  }
  def RaySegment(A: Ray, B: Segment) : Locus = intersect(B, A)
  def SegmentRay(A: Segment, B: Ray) : Locus = {
    if ((A.angle + math.Pi) % math.Pi === (B.angle + math.Pi) % math.Pi) {
      (B contains A.p1, B contains A.p2) match {
        case (true, true) => A
        case (true, false) => if (B.p1 != A.p1) Segment(B.p1, A.p1) else B.p1
        case (false, true) => if (B.p1 != A.p2) Segment(B.p1, A.p2) else B.p1
        case (false, false) => Union(Set())
      }
    }
    else {
      val List(p) = intersect(A.asLine, B.asLine).asPoints
      if ((A contains p) && (B contains p)) p
      else Union(Set())
    }
  }
  def LineSegment(A: Line, B: Segment) : Locus = intersect(B, A)
  def SegmentLine(A: Segment, B: Line) : Locus = {
    if ((A.angle + math.Pi) % math.Pi === (B.angle + math.Pi) % math.Pi) {
      if (B contains A.p1) A
      else Union(Set())
    }
    else {
      val List(p) = intersect(A.asLine, B).asPoints
      if (A contains p) p
      else Union(Set())
    }
  }
  def CircleSegment(A: Circle, B: Segment) : Locus = intersect(B, A)
  def SegmentCircle(A: Segment, B: Circle) : Locus = {
    val Union(ps) = intersect(A.asLine, B)
    Union(ps filter {
      case p: Point => A contains p
      case _ => false
    })
  }
}
