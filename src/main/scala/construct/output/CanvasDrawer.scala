package construct.output

import construct.engine._
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import PixelDrawer._

class CanvasDrawer(val canvas: Canvas, val shapes: List[Drawable], val suggestions: List[List[Drawable]]) {
    val size = IPoint(canvas.width, canvas.height)
    val border = 25
    val targetBounds = Box(border, border, size.x - border, size.y - border)
    val bounds = (suggestions map boundingBox).foldLeft(boundingBox(shapes))(_ + _)
    val targetBox = bounds fill targetBounds
    val pointsToPixels = homography(bounds, targetBox)
    val pixelsToPoints = homography(targetBox, bounds)

    val ctx = canvas.getContext("2d")
    .asInstanceOf[dom.CanvasRenderingContext2D]
    ctx.font = "italic 20px serif"

    val boundary = Union(
    Set(
    Segment(Point(0, 0), Point(canvas.width, 0)),
    Segment(Point(canvas.width, 0), Point(canvas.width, canvas.height)),
    Segment(Point(canvas.width, canvas.height), Point(0, canvas.height)),
    Segment(Point(0, canvas.height), Point(0, 0))
    ))

    def setColorFill(c: Color): Unit = {
      ctx.fillStyle = s"rgb(${c.r},${c.g},${c.b})"
    }

    def setColorStroke(c: Color): Unit = {
      ctx.strokeStyle = s"rgb(${c.r},${c.g},${c.b})"
    }

    def setColor(c: Color): Unit = {
      setColorStroke(c)
      setColorFill(c)
    }

    def draw(): (Point => Point) = {
      //ctx.clearRect(0, 0, canvas.width, canvas.height)
      setColorFill(Color.WHITE())
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      shapes foreach {
        drawPerm
      }
      suggestions zip (tmpColors map { c =>
        Scheme(c(), c())
      }) foreach {
        case (drawables, scheme) =>
          drawables foreach { drawable =>
            draw(drawable, scheme)
          }
      }
      pixelsToPoints
    }

  def drawPerm(d: Drawable): Unit = {
    draw(d, Scheme(Color.GRAY(), Color.BLACK()))
  }

  def draw(obj: Drawable, sc: Scheme): Unit = {
    val name = obj.name
    obj.locus match {
      case pt: Point       => drawPoint(name, pointsToPixels(pt), sc)
      case Circle(c, e)    => drawCircle(name, pointsToPixels(c), pointsToPixels(e), sc)
      case Line(p1, p2)    => drawLine(name, pointsToPixels(p1), pointsToPixels(p2), sc)
      case Ray(p1, p2)     => drawRay(name, pointsToPixels(p1), pointsToPixels(p2), sc)
      case Segment(p1, p2) => drawSegment(name, pointsToPixels(p1), pointsToPixels(p2), sc)
      case Union(loci) => {
        val loci_list = loci.toList
        loci_list.headOption map { Drawable(name, _) } foreach { draw(_, sc) }
        loci_list.tail map { Drawable("", _) } foreach { draw(_, sc) }
      }
    }
  }

  def drawPoint(name: String, pt: Point, scheme: Scheme) = {
    if (!(name startsWith "TmpItem")) {
      val Point(x, y) = pt
      val r = 4
      ctx.beginPath()
      ctx.lineWidth = 1.0
      ctx.arc(x, y, r, 0, 2 * math.Pi)
      setColor(scheme.draw)
      ctx.fill()
      setColor(scheme.label)
      ctx.fillText(name, x+r, y + 2 * r)
    }
  }

  def drawCircle(name: String, c: Point, e: Point, scheme: Scheme) = {
    val r_v = e - c
    val ang = math.Pi / 4
    val buf = 10
    val thick = 1.5f
    val Point(lx, ly) = (r_v rotate ang) * ((!r_v + buf) / !r_v) + c

    setColor(scheme.draw)
    ctx.lineWidth = thick
    ctx.beginPath()
    ctx.arc(c.x, c.y, !r_v, 0, 2 * math.Pi)
    ctx.stroke()
    setColor(scheme.label)
    ctx.fillText(name, lx, ly)
  }

  def drawSegment(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val Point(x1, y1) = p1
    val Point(x2, y2) = p2
    val Point(lx, ly) = Point(buf, buf) + (p1 + p2) / 2
    setColor(scheme.draw)
    ctx.lineWidth = thick
    ctx.beginPath()
    ctx.moveTo(x1, y1)
    ctx.lineTo(x2, y2)
    ctx.stroke()
    setColor(scheme.label)
    ctx.fillText(name, lx, ly)
  }

  def drawRay(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val Point(x1, y1) = p1
    val List(Point(x3, y3)) = (Ray(p1, p2) intersect boundary).asPoints
    val Point(lx, ly) = Point(buf, buf) + (p2 - p1) / 3 + p1
    setColor(scheme.draw)
    ctx.lineWidth = thick
    ctx.beginPath()
    ctx.moveTo(x1, y1)
    ctx.lineTo(x3, y3)
    ctx.stroke()
    setColor(scheme.label)
    ctx.fillText(name, lx, ly)
  }

  def drawLine(name: String, p1: Point, p2: Point, scheme: Scheme) = {
    val buf = 10
    val thick = 1.5f
    val Point(x1, y1) = p1
    val Point(x2, y2) = p2
    val List(Point(x3, y3), Point(x4, y4)) =
      (Line(p1, p2) intersect boundary).asPoints
    val Point(lx, ly) = Point(buf, buf) + (p1 + p2) / 2
    setColor(scheme.draw)
    ctx.lineWidth = thick
    ctx.beginPath()
    ctx.moveTo(x3, y3)
    ctx.lineTo(x4, y4)
    ctx.stroke()
    setColor(scheme.label)
    ctx.fillText(name, lx, ly)
  }

}
