package construct.output

import construct.engine._
import scala.collection.mutable.HashMap

case class Id(name: String)

class Draw {
  var lastId = -1
  val idPrefix = "tmp"

  def nextId() : String = {
    lastId += 1
    s"$idPrefix$lastId"
  }

  def hiddenPoint(p: Point) : (Id, String) = {
    val Point(x, y) = p
    val id = nextId()
    (Id(id), tkzHiddenPoint(id, x, y))
  }

  def hiddenPairOfPoints(p1: Point, p2: Point) : ((Id, Id), String) = {
    val (p1_id, p1_str) = hiddenPoint(p1)
    val (p2_id, p2_str) = hiddenPoint(p2)
    ((p1_id, p2_id), p1_str + p2_str)
  }

  def shapeFromPointPair(p1: Point,
                         p2: Point,
                         fn: (String, String) => String)
                         : String = {
    val ((Id(id1),Id(id2)),str) = hiddenPairOfPoints(p1, p2)
    str + fn(id1, id2)
  }

  def draw(drawable: Drawable) : String = {
    val name = drawable.name
    drawable.locus match {
      case Circle(c, e) => shapeFromPointPair(c, e, tkzCircle(_,_))
      case Line(p1, p2) => shapeFromPointPair(p1, p2, tkzLine(_,_))
      case Segment(p1, p2) => shapeFromPointPair(p1, p2, tkzSegment(_,_))
      case Ray(p1, p2) => shapeFromPointPair(p1, p2, tkzRay(_,_))
      case Point(x, y) => tkzPoint(name, x, y)
      case Union(loci) => drawMultiple(loci map { Drawable(nextId(),_) })
    }
  }

  def drawMultiple(drawables: Iterable[Drawable]) : String =
    drawables map { draw(_) } mkString "\n"

  def draw(drawables: Iterable[Drawable]) : String =
    header + env("document") {env("tikzpicture") { drawMultiple(drawables) }}

  def env(env: String)(body: String) : String = {
    s"""
    \\begin{$env} $body
    \\end{$env} """
  }

  def tkzHiddenPoint(name: String, x: Double, y: Double) : String =
    f"""
    \\tkzDefPoint($x%.2f,$y%.2f){$name}"""

  def tkzPoint(name: String, x: Double, y: Double) : String =
    f"""
    \\tkzDefPoint($x%.2f,$y%.2f){$name}
    \\tkzDrawPoints($name)
    \\tkzLabelPoints($name) """

  val header = s"""
  \\documentclass{article}

  \\usepackage[utf8]{inputenc}
  \\usepackage[upright]{fourier}
  \\usepackage[usenames,dvipsnames,svgnames]{xcolor}
  \\usepackage{tkz-euclide,fullpage}
  \\usetkzobj{all}
  \\usepackage[frenchb]{babel}
  \\definecolor{fondpaille}{cmyk}{0,0,0.1,0}
  \\tkzSetUpColors[background=fondpaille,text=Maroon]  """

  def tkzSegment(p1: String, p2: String) : String =
    f"""
    \\tkzDrawSegment($p1,$p2) """

  def tkzRay(p1: String, p2: String) : String =
    f"""
    \\tkzDrawLine($p1,$p2) """

  def tkzLine(p1: String, p2: String) : String =
    f"""
    \\tkzDrawLine($p1,$p2) """

  def tkzCircle(center: String, edge: String) : String =
    f"""
    \\tkzDrawCircle($center,$edge) """

  def tkzGetPoints(pts: List[String]) : String = {
    val pts_str = pts map {"{" + _ + "}"} mkString ""
    if (pts.length > 1) {
      s"""
      \\tkzGetPoints$pts_str """
    } else {
      s"""
      \\tkzGetPoint$pts_str """
    }
  }

  def tkzInter(t: String)
              (c1: String, e1: String, c2: String, e2:String, i: List[String]) : String = {
    f"""\\tkzInter$t($c1,$e1)($c2,$e2)
    ${tkzGetPoints(i)}
    \\tkzDrawPoints(${i mkString ","})
    \\tkzLabelPoints(${i mkString ","}) """
  }

  val tkzInterLL = tkzInter("LL") _
  val tkzInterLC = tkzInter("LC") _
  val tkzInterCC = tkzInter("CC") _
}

object TkzEuclide {
  def render(drawables: Iterable[Drawable]) : String = {
    val drawer = new Draw
    drawer.draw(drawables)
  }
}
