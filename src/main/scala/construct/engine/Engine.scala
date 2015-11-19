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
  def choose : Option[Point]
}

sealed abstract class PrimativeLocus extends SingleLocus {
  def intersect(other: Locus) : Locus
  def choose : Option[Point]
}

case class Union(val set: Set[SingleLocus]) extends Locus {

  override def asPoints : List[Point] = (set collect {case x: Point => x}).toList
  override def asLines : List[Line] = (set collect {case x: Line => x}).toList
  override def asCircles : List[Circle] = (set collect {case x: Circle => x}).toList

  override def equals(other: Any) : Boolean = {
    other match {
      case union: Union => set == union.set
      case _ => false
    }
  }

  def intersect(other: Locus) : Locus = {
    set map { _ intersect other } reduce { _ union _ }
  }

  def union(other: Locus) : Locus = {
    if (this == other) this
    else if (this == Union(Set())) other
    else other match {
      case single: SingleLocus => Union(set + single)
      case union: Union => Union(union.set ++ set)
    }
  }

  def choose : Option[Point] = {
    if (set.isEmpty) None
    else set.head.choose
  }

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
  def intersect(other: Locus) : Locus = other intersect this
  def choose : Option[Point] = Some(this)
  override def asPoints : List[Point] = List(this)
  def +(that: Point) : Point = Point(this.x + that.x, this.y + that.y)
  def -(that: Point) : Point = Point(this.x - that.x, this.y - that.y)
  def *(that: Double) : Point = Point(this.x * that, this.y * that)
  def /(that: Double) : Point = Point(this.x / that, this.y / that)
  def /(that: Point) : Option[Double] = {
    if ((angle - that.angle) % 180 === 0) {
      Some(if (x == 0) y / that.y
           else x / that.x)
    }
    else None
  }
  def <>(that: Point) : Double = x * that.x + y * that.y
  def project(that: Point) : Point = that * ((this <> that) / math.pow(!that, 2))
  def reject(that: Point) : Point = this - this project that
  def rotate(ang: Double) : Point = {
    val s = math.sin(ang)
    val c = math.cos(ang)
    Point(x * c - y * s, y * c + x * s)
  }
  def withMag(mag: Double) = this * (mag / !this)
  def unary_!() : Double = math.sqrt(x * x + y * y)
  def angle : Double = math.atan2(x, y)
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

  def intersect(other: Locus) : Locus = {
    other match {
      case line: Line => line intersectCircle this
      case circle: Circle => this intersectCircle circle
      case union: Union => union intersect this
      case point: Point => if (this contains point) { Union(Set(point)) } else { Union(Set()) }
    }
  }
  def intersectCircle(other: Circle) : Locus = {
    if (this == other) this
    else {
      val dist = !(c - other.c)
      if (r_sq < other.r_sq) other intersectCircle this
      else {
        val r_towards_other = (other.c - c) * r / dist
        if (r + other.r === dist || other.r + dist === r)
          c + r_towards_other
        else if (r + other.r < dist || other.r + dist < r)
          Union(Set())
        else {
          val ang = math.acos((r_sq + dist * dist - other.r_sq) / (2 * r * dist))
          Union(Set(c + r_towards_other.rotate(ang), c + r_towards_other.rotate(-ang)))
        }
      }
    }
  }
  def choose : Option[Point] = Some(p)
  override def asCircles : List[Circle] = List(this)
}

case class Line(val p1: Point, val p2: Point) extends PrimativeLocus {
  import Closeness._
  val angle = math.atan2(p2.y - p1.y, p2.y - p1.x)
  val standard_form = if (p1.x === p2.x) None
                      else {
                        val slope = (p2.y - p1.y) / (p2.x - p1.x)
                        Some(slope, p1.y - slope * p1.x)
                      }

  def param(pt: Point) : Option[Double] = (p2 - p1) / (pt - p1)

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

  def intersect(other: Locus) : Locus = {
    other match {
      case line: Line => this intersectLine line
      case circle: Circle => this intersectCircle circle
      case union: Union => union intersect this
      case point: Point => this intersectPoint point
    }
  }

  def contains(pt: Point) : Boolean = !((pt - p1) reject (p2 - p1)) === 0

  def intersectPoint(pt: Point) : Locus = {
    if (this contains pt)
      pt
    else
      Union(Set())
  }

  def intersectLine(other: Line) : Locus = {
    (standard_form, other.standard_form) match {
      case (Some((s, y)), Some((other_s, other_y))) => {
        if (s === other_s)
          if (y === other_y)
            this
          else
            Union(Set())
        else {
          val x = (y - other_y) / (other_s - s)
          Point(x, y + s * x)
        }
      }
      case (None, Some((_,_))) => other intersectLine this
      case (Some((slope, y0)),None) => Point(other.p1.x, y0 + other.p1.x * slope)
      case (None, None) => {
        if (p1.x === other.p1.x)
          this
        else
          Union(Set())
      }
    }
  }
  def intersectCircle(circ: Circle) : Locus = {
    standard_form match {
      case Some((m, b)) => {
        // Line  : y = m * x + b
        // Circle: (y - c.y)^2 + (x - c.x)^2 = r^2
        //
        // Solution: Find x in
        // Ax^2 + Bx + C = 0
        val A = math.pow(m,2) + 1
        val B = 2 * (b - circ.c.y) * m - 2 * circ.c.x
        val C = math.pow(b - circ.c.y, 2) + math.pow(circ.c.x, 2) - circ.r_sq
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
        val line_x = p1.x
        if (line_x === circ.c.x - circ.r || line_x === circ.c.x + circ.r) {
          Point(line_x, circ.c.y)
        } else if (math.abs(line_x - circ.c.x) < circ.r) {
          val y_diff = math.sqrt(circ.r_sq - math.pow(line_x - circ.c.x,2))
          Union(Set(Point(line_x,circ.c.y + y_diff), Point(line_x, circ.c.y - y_diff)))
        } else {
          Union(Set())
        }
      }
    }
  }
  def choose : Option[Point] = Some(p2)
  override def asLines : List[Line] = List(this)
}
