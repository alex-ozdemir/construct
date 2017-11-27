package construct.output

import construct.engine._
import construct.engine.Closeness._

case class Box(x1: Double, y1: Double, x2: Double, y2: Double) {
  def +(that: Box) =
    Box(math.min(this.x1, that.x1),
      math.min(this.y1, that.y1),
      math.max(this.x2, that.x2),
      math.max(this.y2, that.y2))
  val center = Point((x1 + x2) / 2, (y1 + y2) / 2)
  val min_vec: Point = Point(x1, y1) - center
  val max_vec: Point = Point(x2, y2) - center
  def *(scale: Double) = {
    val ul_new = min_vec * scale + center
    val br_new = max_vec * scale + center
    Box(ul_new.x, ul_new.y, br_new.x, br_new.y)
  }

  /**
    * Returns a new box with the aspect ratio of this one scaled to fit that one
    */
  def fill(that: Box): Box = {
    val x_ratio = (that.x2 - that.x1) / (this.x2 - this.x1)
    val y_ratio = (that.y2 - that.y1) / (this.y2 - this.y1)
    var ratio = math.min(x_ratio, y_ratio)
    if (ratio == 0 || ratio.isNaN() || ratio.isInfinite()) {
      ratio = 1
    }
    val ul_new = min_vec * ratio + that.center
    val br_new = max_vec * ratio + that.center
    Box(ul_new.x, ul_new.y, br_new.x, br_new.y)
  }

}


object PixelDrawer {
  val tmpColors = List(Color.BLUE,
    Color.RED,
    Color.CYAN,
    Color.GREEN,
    Color.MAGENTA,
    Color.ORANGE,
    Color.YELLOW,
    Color.PINK)

  val pinf: Double = Double.PositiveInfinity
  val ninf: Double = Double.NegativeInfinity
  val trivialBox = Box(pinf, pinf, ninf, ninf)

  def box(p: Point): Box = Box(p.x, p.y, p.x, p.y)

  def box(obj: Drawable): Box = {
    obj match {
      case Drawable(_, p: Point)        => box(p)
      case Drawable(_, Line(p1, p2))    => box(p1) + box(p2)
      case Drawable(_, Segment(p1, p2)) => box(p1) + box(p2)
      case Drawable(_, Ray(p1, p2))     => box(p1) + box(p2)
      case Drawable(_, Circle(c, e)) => {
        val r = !(c - e)
        box(c + Point(r, 0)) + box(c + Point(-r, 0)) + box(c + Point(0, r)) + box(
          c + Point(0, -r))
      }
      case Drawable(_, Union(loci)) =>
        loci
          .map { l =>
            box(Drawable("", l))
          }
          .foldLeft(trivialBox) { _ + _ }
    }
  }

  def boundingBox(drawables: List[Drawable]): Box = {
    drawables.map { box }.foldLeft(trivialBox)(_ + _) + Box(-1, -1, 1, 1)
  }

  def homography(domain: Box, image: Box): Point => Point = {
    val x_out = image.x2 - image.x1
    val x_in = domain.x2 - domain.x1
    val y_out = image.y2 - image.y1
    val y_in = domain.y2 - domain.y1
    var x_ratio = if (x_in === 0) 1 else x_out / x_in
    var y_ratio = if (y_in === 0) 1 else y_out / y_in
    return { x: Point =>
      {
        val v = x - domain.center
        val v_new = Point(v.x * x_ratio, v.y * y_ratio)
        image.center + v_new
      }
    }
  }
}

case class IPoint(x: Int, y: Int)
case class Scheme(draw: Color, label: Color)

sealed abstract class Color(val r: Int, val g: Int, val b: Int)
object Color {
  case class BLUE() extends Color(0, 0, 255)
  case class RED() extends Color (255, 0, 0)
  case class CYAN() extends Color (51, 224, 204)
  case class GREEN() extends Color (0, 153, 51)
  case class MAGENTA() extends Color (102, 0, 255)
  case class ORANGE() extends Color (255, 153, 0)
  case class YELLOW() extends Color (204, 204, 0)
  case class PINK() extends Color (204, 0, 204)
  case class BLACK() extends Color (0, 0, 0)
  case class GRAY() extends Color (128, 128, 128)
  case class WHITE() extends Color (255, 255, 255)
}


