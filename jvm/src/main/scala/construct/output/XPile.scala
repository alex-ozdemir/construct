package construct.output

import construct.input.ast._

import scala.collection.mutable
import scala.collection.mutable.{HashMap, HashSet, MutableList, Queue}

class XPilerError(val msg: String) extends RuntimeException(s"Error: $msg")

object XPiler {
  def asMacro(c: Construction): String = {
    val proc = new ConstructionProcessor(c, true)
    proc.emitMacro
  }
  def asPicture(c: Construction): String = {
    val proc = new ConstructionProcessor(c, false)
    proc.emitPicture
  }
  def asDoc(main: Construction, lib: Iterable[Item]): String = {
    val cons = mutable.HashMap[Identifier, Construction](main.name -> main)
    val shapes = mutable.HashSet[Identifier]()
    // NB(aozdemir) the unit types are here to prevent type inference problems
    lib foreach {
      case c: Construction =>
        cons += (c.name -> c)
        ()
      case s: Shape        =>
        shapes += s.con.name
        ()
    }
    def lookupConstruction(id: Identifier,
                           s: Statement): Option[Construction] = {
      val builtins =
        List("circle", "line", "segment", "ray", "new", "intersection") map {
          Identifier
        }
      if (builtins contains id) None
      else {
        Some(cons.getOrElse(id, {
          if (shapes contains id)
            throw new XPilerError(
              s"Identifier <${id.name}> is a shape, but can only XPile constructions.\nStatement: $s")
          else
            throw new XPilerError(
              s"Unknown Identifier <${id.name}> in statement $s")
        }))
      }
    }
    val used_constructions = mutable.MutableList[Construction]()
    val constructions_to_scan = mutable.Queue[Construction](main)
    while (constructions_to_scan.nonEmpty) {
      val con = constructions_to_scan.dequeue()
      used_constructions += con
      con.statements foreach {
        case s @ Statement(_, FnApp(fn_id, params)) =>
          if (params forall {
                case Exactly(id) => true
                case _           => false
              }) {
            lookupConstruction(fn_id, s) foreach { c =>
              constructions_to_scan.enqueue(c)
            }
          } else {
            throw new XPilerError(
              s"Application of ${fn_id.name} to args $params include non-identifier parameters!")
          }
        case _ =>
      }
    }
    header + env("document") {
      (used_constructions filter { _ != main } map { asMacro } mkString "\n") + "\n" +
        asPicture(main)
    }
  }
  def env(env: String)(body: String): String = {
    s"""
    \\begin{$env}
      $body
    \\end{$env} """
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
}

class ConstructionProcessor(c: Construction, asMacro: Boolean) {
  val Construction(name, params, statements, returns) = c
  val constructions = mutable.HashMap[Identifier, String]()
  val points = mutable.HashMap[Identifier, String]()
  val circles = mutable.HashMap[Identifier, String]()
  val lines = mutable.HashMap[Identifier, String]()
  def getPoint(id: Identifier): String = points.getOrElse(id, {
    throw new Error("!")
  })
  require(params forall { case Parameter(_, ty) => ty == Identifier("point") })
  require(returns.length <= 2)
  var i = 1
  if (asMacro) {
    params foreach {
      case Parameter(name, _) =>
        points += (name -> s"#$i")
        i += 1
    }
  } else {
    params foreach { case Parameter(name, _) => points += (name -> name.name) }
  }
  def nameId(i: Identifier): String =
    if (asMacro) s"$localPrefix${i.name}" else i.name
  val localPrefix = s"${c.name.name.replace("_", "")}Tmp"
  def nameCons(i: Identifier): String = s"\\construct${i.name.replace("_", "")}"
  def makeStatement(s: Statement): String = {
    val INTER = Identifier("intersection")
    val CIRC = Identifier("circle")
    val LINE = Identifier("line")
    s match {
      case Statement(Id(id), FnApp(CIRC, List(Exactly(id1), Exactly(id2)))) =>
        circles += (id -> s"${getPoint(id1)},${getPoint(id2)}")
        ""
      case Statement(Id(id), FnApp(LINE, List(Exactly(id1), Exactly(id2)))) =>
        lines += (id -> s"${getPoint(id1)},${getPoint(id2)}")
        ""
      case Statement(p, FnApp(INTER, List(Exactly(id1), Exactly(id2)))) =>
        val lineIds = List(id1, id2) filter { id =>
          lines contains id
        }
        val circleIds = List(id1, id2) filter { id =>
          circles contains id
        }
        val inter = (lineIds.length, circleIds.length) match {
          case (0, 2) =>
            val str0 = circles.getOrElse(circleIds(0), {
              throw new Error("!")
            })
            val str1 = circles.getOrElse(circleIds(1), {
              throw new Error("!")
            })
            s"\\tkzInterCC($str0)($str1)"
          case (1, 1) =>
            val str0 = lines.getOrElse(lineIds(0), {
              throw new Error("!")
            })
            val str1 = circles.getOrElse(circleIds(0), {
              throw new Error("!")
            })
            s"\\tkzInterLC($str0)($str1)"
          case (2, 0) =>
            val str0 = lines.getOrElse(lineIds(0), {
              throw new Error("!")
            })
            val str1 = lines.getOrElse(lineIds(1), {
              throw new Error("!")
            })
            s"\\tkzInterLL($str0)($str1)"
          case _ =>
            throw new Error(s"Cannot take intersection of these types: $s")
        }
        inter + " " + emitPattern(p)
      case Statement(p, FnApp(fn_id, params)) =>
        nameCons(fn_id) +
          (params map {
            case Exactly(id) => s"{${getPoint(id)}}"
            case pa =>
              throw new XPilerError(
                s"Cannot do construction call with non-point $pa")
          } mkString "") + " " +
          emitPattern(p)
    }
  }
  def emitPattern(p: Pattern): String = p match {
    case Id(id) =>
      points += (id -> nameId(id))
      s"\\tkzGetPoint{${nameId { id }}}"
    case Tuple(List(Id(id1), Id(id2))) =>
      points += (id1 -> nameId(id1))
      points += (id2 -> nameId(id2))
      s"\\tkzGetPoints{${nameId { id1 }}}{${nameId { id2 }}}"
    case p => throw new XPilerError(s"Can not XPile the complex pattern $p")
  }
  def emitReturns: String =
    c.returns match {
      case List(id) => s"\\tkzRenamePoint(${getPoint(id)}){tkzPointResult}"
      case List(id1, id2) =>
        s"\\tkzRenamePoint(${getPoint(id1)}){tkzFirstPointResult}\n" +
          s"\\tkzRenamePoint(${getPoint(id2)}){tkzSecondPointResult}"
      case _ => throw new Error("Can only return 2 points")
    }
  def emitBody: String =
    statements map { makeStatement } filter { _ != "" } mkString "\n"
  def emitMacro: String =
    s"\\newcommand{${nameCons(name)}}[${params.length}]{\n" +
      emitBody + "\n" + emitReturns + "\n}"
  def emitPicture: String = {
    val pointsPos = List((0, 0), (1, 0), (1, 1))
    env("tikzpicture") {
      (c.parameters zip pointsPos map {
        case (Parameter(name, _), (x, y)) =>
          f"\\tkzDefPoint($x%.2f,$y%.2f){${name.name}}"
      } mkString "\n") + "\n" + emitBody +
        s"\n\\tkzDrawPoints(${points.values mkString ","})" +
        s"\n\\tkzLabelPoints(${points.values mkString ","})" +
        (if (lines.nonEmpty) s"\n\\tkzDrawLines(${lines.values mkString " "})"
         else "") +
        "\n" + (circles.values map { str =>
        s"\\tkzDrawCircle($str)"
      } mkString "\n")
    }
  }
  def env(env: String)(body: String): String = {
    s"""
    \\begin{$env}
      $body
    \\end{$env} """
  }
}
