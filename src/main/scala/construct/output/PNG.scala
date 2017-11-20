package construct.output

import construct.input.ast._
import construct.engine._
import construct.engine.Closeness._

import scala.collection.mutable.HashMap

import java.awt.image.BufferedImage
import java.awt.{Graphics2D, Color, Font, BasicStroke, RenderingHints}
import java.awt.geom._

case class IPoint(val x: Int, val y: Int)
case class Scheme(val draw: Color, val label: Color)

class Drawer(val size: IPoint, val trans: (Point => Point)) {
  val canvas = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB)
  val boundary = Union(
    Set(
      Segment(Point(0, 0), Point(size.x, 0)),
      Segment(Point(size.x, 0), Point(size.x, size.y)),
      Segment(Point(size.x, size.y), Point(0, size.y)),
      Segment(Point(0, size.y), Point(0, 0))
    ))
  val graphics = canvas.createGraphics()
  val font = new Font("Serif", Font.ITALIC, 20)
  val perm = Scheme(Color.GRAY, Color.BLACK)

  {
    graphics.setColor(Color.WHITE)
    graphics.fillRect(0, 0, canvas.getWidth, canvas.getHeight)
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
  }

  def drawPerm(drawable: Drawable) = draw(drawable, perm)

  def draw(obj: Drawable, sc: Scheme): Unit = {
    val name = obj.name
    obj.locus match {
      case pt: Point       => drawPoint(name, pt, sc)
      case Circle(c, e)    => drawCircle(name, c, e, sc)
      case Line(p1, p2)    => drawLine(name, p1, p2, sc)
      case Ray(p1, p2)     => drawRay(name, p1, p2, sc)
      case Segment(p1, p2) => drawSegment(name, p1, p2, sc)
      case Union(loci) => {
        val loci_list = loci.toList
        loci_list.headOption map { Drawable(name, _) } map { draw(_, sc) }
        loci_list.tail map { Drawable("", _) } foreach { draw(_, sc) }
      }
    }
  }

  def drawPoint(name: String, pt: Point, scheme: Scheme) = {
    if (!(name startsWith "TmpItem")) {
      val Point(x, y) = trans(pt)
      val r = 4
      graphics.setColor(scheme.draw)
      graphics.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
      graphics.setColor(scheme.label)
      graphics.setFont(font)
      graphics.drawString(name, (x + r).toFloat, (y + 2 * r).toFloat)
    }
  }

  def drawCircle(name: String, c: Point, e: Point, scheme: Scheme) = {
    val r_v = trans(e) - trans(c)
    val ang = math.Pi / 4
    val r = !(trans(c) - trans(e))
    val Point(x, y) = trans(c)
    val buf = 10
    val thick = 1.5f
    val Point(lx, ly) = (r_v rotate ang) * ((!r_v + buf) / !r_v) + trans(c)
    graphics.setColor(scheme.draw)
    graphics.setStroke(new BasicStroke(thick))
    graphics.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
    graphics.setColor(scheme.label)
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def drawSegment(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val Point(x1, y1) = trans(p1)
    val Point(x2, y2) = trans(p2)
    val Point(lx, ly) = Point(buf, buf) + (trans(p1) + trans(p2)) / 2
    graphics.setColor(scheme.draw)
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)
    graphics.setColor(scheme.label)
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def drawRay(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val p1t @ Point(x1, y1) = trans(p1)
    val p2t @ Point(x2, y2) = trans(p2)
    val List(Point(x3, y3)) = (Ray(p1t, p2t) intersect boundary).asPoints
    val Point(lx, ly) = Point(buf, buf) + (p2t - p1t) / 3 + p1t
    graphics.setColor(scheme.draw)
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x1.toInt, y1.toInt, x3.toInt, y3.toInt)
    graphics.setColor(scheme.label)
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def drawLine(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val p1t @ Point(x1, y1) = trans(p1)
    val p2t @ Point(x2, y2) = trans(p2)
    val List(Point(x3, y3), Point(x4, y4)) =
      (Line(p1t, p2t) intersect boundary).asPoints
    val Point(lx, ly) = Point(buf, buf) + (p1t + p2t) / 2
    graphics.setColor(scheme.draw)
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x4.toInt, y4.toInt, x3.toInt, y3.toInt)
    graphics.setColor(scheme.label)
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def write(file: String) = {
    graphics.dispose()
    javax.imageio.ImageIO.write(canvas, "png", new java.io.File(file))
  }

  def get: BufferedImage = canvas
}

case class Box(x1: Double, y1: Double, x2: Double, y2: Double) {
  def +(that: Box) =
    Box(math.min(this.x1, that.x1),
        math.min(this.y1, that.y1),
        math.max(this.x2, that.x2),
        math.max(this.y2, that.y2))
  val center = Point((x1 + x2) / 2, (y1 + y2) / 2)
  val min_vec = Point(x1, y1) - center
  val max_vec = Point(x2, y2) - center
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

object PNG {

  val pinf = Double.PositiveInfinity
  val ninf = Double.NegativeInfinity
  val trivialBox = Box(pinf, pinf, ninf, ninf)
  val tmpColors = List(Color.BLUE,
                       Color.RED,
                       Color.CYAN,
                       Color.GREEN,
                       Color.MAGENTA,
                       Color.ORANGE,
                       Color.YELLOW,
                       Color.PINK)

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
    drawables.map { box(_) }.foldLeft(trivialBox)(_ + _) + Box(1, 1, -1, -1)
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

  def dump(drawables: List[Drawable], file: String) =
    dumpTmp(drawables, List(), file)

  /** Draw the `drawables`.
    *
    *  Returns:
    *    * `BufferedImage`: the drawing
    *    * `Point => Point`: A mapping from pixels to the cartesian coordinate
    *       system of the `drawables`.
    */
  def get(drawables: List[Drawable]): (BufferedImage, Point => Point) =
    getTmp(drawables, List())

  def getTmp(perm: List[Drawable],
             tmp: List[List[Drawable]]): (BufferedImage, Point => Point) = {
    val size = IPoint(500, 500)
    val border = 25
    val targetBounds = Box(border, border, size.x - border, size.y - border)
    val bounds = (tmp map boundingBox).foldLeft(boundingBox(perm))(_ + _)
    val targetBox = bounds fill targetBounds
    val trans = homography(bounds, targetBox)
    val revereseHomography = homography(targetBox, bounds)
    val drawer = new Drawer(size, trans)
    perm foreach { drawer.drawPerm(_) }
    tmp zip (tmpColors map { c =>
      Scheme(c, c)
    }) foreach {
      case (drawables, scheme) =>
        drawables foreach { drawable =>
          drawer.draw(drawable, scheme)
        }
    }
    (drawer.get, revereseHomography)
  }

  def dumpTmp(perm: List[Drawable], tmp: List[List[Drawable]], file: String) = {
    val size = IPoint(500, 500)
    val border = 25
    val targetBounds = Box(border, border, size.x - border, size.y - border)
    val bounds = (tmp map boundingBox).foldLeft(boundingBox(perm))(_ + _)
    val targetBox = bounds fill targetBounds
    val trans = homography(bounds, targetBox)
    val drawer = new Drawer(size, trans)
    perm foreach { drawer.drawPerm(_) }
    tmp zip (tmpColors map { c =>
      Scheme(c, c)
    }) foreach {
      case (drawables, scheme) =>
        drawables foreach { drawable =>
          drawer.draw(drawable, scheme)
        }
    }
    drawer.write(file)
  }
}
