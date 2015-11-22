package construct.output

import construct.input.ast._
import construct.engine._
import scala.collection.mutable.HashMap

object TkzEuclide {

  def dump(objects: HashMap[Identifier,NamedObject]) : String = {
    val pts = objects collect {
      case (Identifier(name), NamedPoint(_, Point(x, y))) => (name, x, y)
    }
    val lines = objects collect {
      case (Identifier(id), NamedLine(_, NamedPoint(name_p1, _), NamedPoint(name_p2, _)))
              => (id, (name_p1, name_p2))
    }
    val rays = objects collect {
      case (Identifier(id), NamedRay(_, NamedPoint(name_p1, _), NamedPoint(name_p2, _)))
              => (id, (name_p1, name_p2))
    }
    val segments = objects collect {
      case (Identifier(id), NamedSegment(_, NamedPoint(name_p1, _), NamedPoint(name_p2, _)))
              => (id, (name_p1, name_p2))
    }
    val circles = objects collect {
      case (Identifier(id), NamedCircle(_, NamedPoint(name_c, _), NamedPoint(name_e, _)))
              => (id, (name_c, name_e))
    }
    header + env("document") {env("tikzpicture") {
      (pts map {case (n,x,y) => tkzPoint(n,x,y)} mkString "\n") +
      (lines.values map {case (p1,p2) => tkzLine(p1,p2) } mkString "\n") +
      (rays.values map {case (p1,p2) => tkzRay(p1,p2) } mkString "\n") +
      (segments.values map {case (p1,p2) => tkzSegment(p1,p2) } mkString "\n") +
      (circles.values map {case (p1,p2) => tkzCircle(p1,p2)} mkString "\n")
    }
  }}

  def env(env: String)(body: String) : String = {
    s"""
    \\begin{$env} $body
    \\end{$env} """
  }

  def tkzPoint(name: String, x: Double, y: Double) : String = {
    if (name startsWith "TmpItem") {
    f"""
    \\tkzDefPoint($x%.2f,$y%.2f){$name}"""
    }
    else {
    f"""
    \\tkzDefPoint($x%.2f,$y%.2f){$name}
    \\tkzDrawPoints($name)
    \\tkzLabelPoints($name) """
    }
  }

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

  def tkzSegment(p1: String, p2: String) : String = {
    f"""
    \\tkzDrawSegment($p1,$p2) """
  }

  def tkzRay(p1: String, p2: String) : String = {
    f"""
    \\tkzDrawLine($p1,$p2) """
  }

  def tkzLine(p1: String, p2: String) : String = {
    f"""
    \\tkzDrawLine($p1,$p2) """
  }

  def tkzCircle(center: String, edge: String) : String = {
    f"""
    \\tkzDrawCircle($center,$edge) """
  }

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
