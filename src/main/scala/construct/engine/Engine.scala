package construct.engine;

import scala.math;

object Closeness {
  val EPSILON = 0.000001
  implicit class CloseDouble(x: Double) {
    def ===(that: Double) : Boolean = math.abs(x - that) < EPSILON
  }
}

sealed abstract class PrimativeLocus {
  def intersect(other: PrimativeLocus) : Set[Point]
}

case class Point(val x: Double, val y: Double) {
  import Closeness._
  override def equals(other: Any) : Boolean = {
    other match {
      case p: Point => math.sqrt( (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y)) === 0
      case _ => false
    }
  }
  def +(that: Point) : Point = Point(this.x + that.x, this.y + that.y)
  def -(that: Point) : Point = Point(this.x - that.x, this.y - that.y)
  def *(that: Double) : Point = Point(this.x * that, this.y * that)
  def /(that: Double) : Point = Point(this.x / that, this.y / that)
  def <>(that: Point) : Double = x * that.x + y * that.y
  def project(that: Point) : Point = that * ((this <> that) / math.pow(!that, 2))
  def rotate(ang: Double) : Point = {
    val s = math.sin(ang)
    val c = math.cos(ang)
    Point(x * c - y * s, y * c + x * s)
  }
  def withMag(mag: Double) = this * (mag / !this)
  def unary_!() : Double = math.sqrt(x * x + y * y)
}

case class Circle(val c: Point, val p: Point) extends PrimativeLocus {
  import Closeness._
  val r_sq = (c.y - p.y) * (c.y - p.y) + (c.x - p.x) * (c.x - p.x)
  val r = math.sqrt(r_sq)
  def intersect(other: PrimativeLocus) : Set[Point] = {
    other match {
      case line: Line => line intersectCircle this
      case circle: Circle => this intersectCircle circle
    }
  }
  def intersectCircle(other: Circle) : Set[Point] = {
    val dist = !(c - other.c)
    if (r_sq < other.r_sq) other intersectCircle this
    else {
      val r_towards_other = (other.c - c) * r / dist
      if (r + other.r === dist || other.r + dist === r) {
        Set(c + r_towards_other)
      }
      else if (r + other.r < dist || other.r + dist < r) {
        Set()
      }
      else {
        val ang = math.acos((r_sq + dist * dist - other.r_sq) / (2 * r * dist))
        Set(c + r_towards_other.rotate(ang), c + r_towards_other.rotate(-ang))
      }
    }
  }
}

case class Line(val p1: Point, val p2: Point) extends PrimativeLocus {
  import Closeness._
  val angle = math.atan2(p2.y - p1.y, p2.y - p1.x)
  val standard_form = if (p1.x === p2.x) None
                      else {
                        val slope = (p2.y - p1.y) / (p2.x - p1.x)
                        Some(slope, p1.y - slope * p1.x)
                      }
  def intersect(other: PrimativeLocus) : Set[Point] = {
    other match {
      case line: Line => this intersectLine line
      case circle: Circle => this intersectCircle circle
    }
  }
  def intersectLine(other: Line) : Set[Point] = {
    (standard_form, other.standard_form) match {
      case (Some((s, y)), Some((other_s, other_y))) => {
        if (s === other_s)
          Set()
        else {
          val x = (y - other_y) / (other_s - s)
          Set(Point(x, y + s * x))
        }
      }
      case (None, Some((_,_))) => other intersectLine this
      case (Some((slope, y0)),None) => Set(Point(other.p1.x, y0 + other.p1.x * slope))
      case (None, None) => Set()
    }
  }
  def intersectCircle(circ: Circle) : Set[Point] = {
    standard_form match {
      case Some((m, b)) => {
        // Line  : y = m * x + b
        // Circle: (y - c.y)^2 + (x - c.x)^2 = r^2
        //
        // Solution:
        // Ax^2 + Bx + C = 0
        val A = math.pow(m,2) + 1
        val B = 2 * (b - circ.c.y) * m - 2 * circ.c.x
        val C = math.pow(b - circ.c.y, 2) + math.pow(circ.c.x, 2) - circ.r_sq
        val desc = math.pow(B,2) - 4 * A * C;
        if (desc === 0) {
          val x = -B / (2 * A);
          Set(Point(x, m * x + b))
        }
        else if (desc < 0) {
          Set()
        }
        else {
          val x1 = (-B - math.sqrt(desc)) / (2 * A)
          val x2 = (-B + math.sqrt(desc)) / (2 * A)
          Set(Point(x1, m * x1 + b), Point(x2, m * x2 + b))
        }
      }
      case None => {
        val line_x = p1.x
        if (line_x === circ.c.x - circ.r || line_x === circ.c.x + circ.r) {
          Set(Point(line_x, circ.c.y))
        } else if (math.abs(line_x - circ.c.x) < circ.r) {
          val y_diff = math.sqrt(circ.r_sq - math.pow(line_x - circ.c.x,2))
          Set(Point(line_x,circ.c.y + y_diff), Point(line_x, circ.c.y - y_diff))
        } else {
          Set()
        }
      }
    }
  }
}
