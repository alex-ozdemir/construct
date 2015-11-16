package construct.output

import construct.input.ast._
import construct.engine._
import construct.engine.Closeness._

import scala.collection.mutable.HashMap

import java.awt.image.BufferedImage
import java.awt.{Graphics2D,Color,Font,BasicStroke}
import java.awt.geom._

case class IPoint(val x: Int, val y: Int)

class Drawer(val size: IPoint, val trans: (Point => Point)) {
  val canvas = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB)
  val graphics = canvas.createGraphics()

  {
    graphics.setColor(Color.WHITE)
    graphics.fillRect(0, 0, canvas.getWidth, canvas.getHeight)
  }

  def draw(obj: NamedObject) = {
    obj match {
      case NamedPoint(name, pt) => drawPoint(name, pt)
      case NamedCircle(name, NamedPoint(_,c), NamedPoint(_,e)) => drawCircle(name, c, e)
      case NamedLine(name, NamedPoint(_,c), NamedPoint(_,e)) => drawLine(name, c, e)
    }
  }

  def drawPoint(name: String, pt: Point) = {
    val Point(x, y) = trans(pt)
    val r = 2
    graphics.setColor(Color.GRAY)
    graphics.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
    graphics.setColor(Color.BLACK)
    graphics.setFont(new Font("Batang", Font.PLAIN, 20))
    graphics.drawString(name, (x + r).toFloat, (y + r).toFloat)
  }

  def drawCircle(name: String, c: Point, e: Point) = {
    val r_v = trans(e) - trans(c)
    val ang = math.Pi / 4
    val r = !(trans(c)-trans(e))
    val Point(x, y) = trans(c)
    val buf = 10
    val Point(lx, ly) = (r_v rotate ang) * ((!r_v + buf) / !r_v) + trans(c)
    graphics.setColor(Color.GRAY)
    graphics.setStroke(new BasicStroke(1f))
    graphics.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r))
    graphics.setColor(Color.BLACK)
    graphics.setFont(new Font("Batang", Font.PLAIN, 20))
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def drawLine(name: String, p1: Point, p2: Point) = {
    val buf = 10
    val Point(x1, y1) = trans(p1)
    val Point(x2, y2) = trans(p2)
    val Point(lx, ly) = Point(buf, buf) + (trans(p1) + trans(p2)) / 2
    graphics.setColor(Color.GRAY)
    graphics.setStroke(new BasicStroke(1f))
    graphics.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)
    graphics.setColor(Color.BLACK)
    graphics.setFont(new Font("Batang", Font.PLAIN, 20))
    graphics.drawString(name, lx.toFloat, ly.toFloat)
  }

  def write(file: String) = {
    graphics.dispose()
    javax.imageio.ImageIO.write(canvas, "png", new java.io.File(file))
  }

  def get : BufferedImage = canvas
}

case class Box(x1: Double, y1: Double, x2: Double, y2: Double) {
  def +(that: Box) = Box(math.min(this.x1, that.x1),
                         math.min(this.y1, that.y1),
                         math.max(this.x2, that.x2),
                         math.max(this.y2, that.y2))
  val center = Point((x1 + x2)/2, (y1 + y2)/2)
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
  def fill(that: Box) : Box = {
    val x_ratio = (that.x2 - that.x1) / (this.x2 - this.x1)
    val y_ratio = (that.y2 - that.y1) / (this.y2 - this.y1)
    val ratio = math.min(x_ratio, y_ratio)
    val ul_new = min_vec * ratio + that.center
    val br_new = max_vec * ratio + that.center
    Box(ul_new.x, ul_new.y, br_new.x, br_new.y)
  }

}

object PNG {

  def box(p: Point) : Box = Box(p.x, p.y, p.x, p.y)

  def box(obj: NamedObject) : Box = {
    obj match {
      case NamedPoint(_,p) => box(p)
      case NamedLine(_,p1,p2) => box(p1) + box(p2)
      case NamedCircle(_,NamedPoint(_,c),NamedPoint(_,e)) => {
        val r = !(c-e)
        box(c+Point(r,0)) + box(c+Point(-r,0)) + box(c+Point(0,r)) + box(c+Point(0,-r))
      }
    }
  }

  def boundingBox(objects: HashMap[Identifier,NamedObject]) : Box = {
    val pinf = Double.PositiveInfinity
    val ninf = Double.NegativeInfinity
    val trivial = Box(pinf,pinf,ninf,ninf)
    objects.values.map{box(_)}.foldLeft(trivial)(_+_)
  }

  def homography(domain: Box, image: Box) : Point => Point = {
    val x_out = image.x2 - image.x1
    val x_in = domain.x2 - domain.x1
    val y_out = image.y2 - image.y1
    val y_in = domain.y2 - domain.y1
    val x_ratio = if (x_in === 0) 1 else x_out / x_in
    val y_ratio = if (y_in === 0) 1 else y_out / y_in
    return { x: Point => {
      val v = x - domain.center
      val v_new = Point(v.x * x_ratio, v.y * y_ratio)
      image.center + v_new
    }}
  }

  def dump(objects: HashMap[Identifier,NamedObject]) = {
    val size = IPoint(500, 500)
    val border = 25
    val targetBounds = Box(border, border, size.x - border, size.y - border)
    val bounds = boundingBox(objects)
    val targetBox = bounds fill targetBounds
    val trans = homography(bounds, targetBox)
    val drawer = new Drawer(size, trans)
    objects foreach {case (id,obj) => drawer.draw(obj)}
    drawer.write("out.png")
  }

  def get(objects: HashMap[Identifier,NamedObject]) : BufferedImage = {
    val size = IPoint(500, 500)
    val border = 25
    val targetBounds = Box(border, border, size.x - border, size.y - border)
    val bounds = boundingBox(objects)
    val targetBox = bounds fill targetBounds
    println(s"Sending box $bounds to $targetBox!")
    val trans = homography(bounds, targetBox)
    val drawer = new Drawer(size, trans)
    objects foreach {case (id,obj) => drawer.draw(obj)}
    drawer.get
  }
}
