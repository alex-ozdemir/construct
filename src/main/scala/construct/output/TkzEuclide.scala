package construct.output

import construct.input.ast._
import construct.engine._
import scala.collection.mutable.HashMap

object TkzEuclide {

  def dump(program: Program,
           points: HashMap[Identifier,Point],
           loci: HashMap[Identifier,PrimativeLocus]) : String = {
    val shape_defs: HashMap[Identifier,(Identifier,Identifier)] = new HashMap()
    header + env("document") {env("tikzpicture")
      { program.statements map {dump(_, points, loci, shape_defs)} mkString "\n" }
    }
  }

  def dump(statement: Statement,
           points: HashMap[Identifier,Point],
           loci: HashMap[Identifier,PrimativeLocus],
           shape_defs: HashMap[Identifier,(Identifier,Identifier)]): String = {
    statement match {
      case IntersectionStatement(id1, id2, ids) => {
        val id_names = ids map {case Identifier(s) => s}
        val (shape1_pt1, shape1_pt2) = shape_defs.get(id1).get
        val (shape2_pt1, shape2_pt2) = shape_defs.get(id2).get
        (loci.get(id1).get, loci.get(id2).get) match {
          case (Circle(c, e), Circle(c2, e2)) => tkzInterCC(shape1_pt1.name,
                                                            shape1_pt2.name,
                                                            shape2_pt1.name,
                                                            shape2_pt2.name,
                                                            id_names)
          case (Circle(c, e), Line(p1, p2)) => tkzInterLC(shape2_pt1.name,
                                                          shape2_pt2.name,
                                                          shape1_pt1.name,
                                                          shape1_pt2.name,
                                                          id_names)
          case (Line(p1, p2), Circle(c, e)) => tkzInterLC(shape1_pt1.name,
                                                          shape1_pt2.name,
                                                          shape2_pt1.name,
                                                          shape2_pt2.name,
                                                          id_names)
          case (Line(p1, p2), Line(q1, q2)) => tkzInterLL(shape1_pt1.name,
                                                          shape1_pt2.name,
                                                          shape2_pt1.name,
                                                          shape2_pt2.name,
                                                          id_names)
        }
      }
      case LetStatement(id, constructor) => dump(constructor, id, points, loci, shape_defs)
      case x : Constructor => dump(x,Identifier("NOPE"), points, loci, shape_defs)
    }
  }

  def dump(constructor: Constructor,
           id: Identifier,
           points: HashMap[Identifier,Point],
           loci: HashMap[Identifier,PrimativeLocus],
           shape_defs: HashMap[Identifier,(Identifier,Identifier)]): String = {
    constructor match {
      case PointConstructor() => {
        val Point(x,y) = points.get(id).get
        tkzPoint(id.name, x, y)
      }
      case LineConstructor(id1, id2) => {
        shape_defs += (id -> (id1, id2))
        tkzLine(id1.name, id2.name)
      }
      case CircleConstructor(id1, id2) => {
        shape_defs += (id -> (id1, id2))
        tkzCircle(id1.name, id2.name)
      }
    }
  }

  def env(env: String)(body: String) : String = {
    s"""
    \\begin{$env} $body
    \\end{$env} """
  }

  def tkzPoint(name: String, x: Double, y: Double) : String = {
    f"""
    \\tkzDefPoint($x%.2f,$y%.2f){$name}
    \\tkzDrawPoints($name)
    \\tkzLabelPoints($name) """
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
