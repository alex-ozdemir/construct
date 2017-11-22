package construct.output

import construct.engine._
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Font, Graphics2D, RenderingHints}
import java.awt
import java.awt.geom._

import construct.output.PixelDrawer._

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


class Drawer(val size: IPoint, val trans: (Point => Point)) {
  val canvas = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB)
  val boundary = Union(
    Set(
      Segment(Point(0, 0), Point(size.x, 0)),
      Segment(Point(size.x, 0), Point(size.x, size.y)),
      Segment(Point(size.x, size.y), Point(0, size.y)),
      Segment(Point(0, size.y), Point(0, 0))
    ))
  val graphics: Graphics2D = canvas.createGraphics()
  val font = new Font("Serif", Font.ITALIC, 20)
  val perm = Scheme(Color.GRAY(), Color.BLACK())

  {
    graphics.setColor(colorToSwing(Color.WHITE()))
    graphics.fillRect(0, 0, canvas.getWidth, canvas.getHeight)
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON)
  }

  def colorToSwing(c: Color): awt.Color =
    new awt.Color(c.r, c.g, c.b)

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
      graphics.setColor(colorToSwing(scheme.draw))
      graphics.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
      graphics.setColor(colorToSwing(scheme.label))
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
    graphics.setColor(colorToSwing(scheme.draw))
    graphics.setStroke(new BasicStroke(thick))
    graphics.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
    graphics.setColor(colorToSwing(scheme.label))
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def drawSegment(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val Point(x1, y1) = trans(p1)
    val Point(x2, y2) = trans(p2)
    val Point(lx, ly) = Point(buf, buf) + (trans(p1) + trans(p2)) / 2
    graphics.setColor(colorToSwing(scheme.draw))
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)
    graphics.setColor(colorToSwing(scheme.label))
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
    graphics.setColor(colorToSwing(scheme.draw))
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x1.toInt, y1.toInt, x3.toInt, y3.toInt)
    graphics.setColor(colorToSwing(scheme.label))
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
    graphics.setColor(colorToSwing(scheme.draw))
    graphics.setStroke(new BasicStroke(thick))
    graphics.drawLine(x4.toInt, y4.toInt, x3.toInt, y3.toInt)
    graphics.setColor(colorToSwing(scheme.label))
    graphics.setFont(font)
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def write(file: String) = {
    graphics.dispose()
    javax.imageio.ImageIO.write(canvas, "png", new java.io.File(file))
  }

  def get: BufferedImage = canvas
}

object PNG {

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
    perm foreach { drawer.drawPerm }
    tmp zip (tmpColors map { c =>
      Scheme(c(), c())
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
      Scheme(c(), c())
    }) foreach {
      case (drawables, scheme) =>
        drawables foreach { drawable =>
          drawer.draw(drawable, scheme)
        }
    }
    drawer.write(file)
  }
}
