package construct.input

import scala.tools.nsc.EvalLoop
import java.awt.image.BufferedImage
import scala.swing._
import java.io.PrintWriter
import scala.collection.mutable.MutableList
import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.ConstructInterpreter
import construct.semantics.ConstructError
import construct.output._
import construct.engine._

class ProgramStore {
  val includes = new MutableList[Path]()
  val parameters = new MutableList[Parameter]()
  val statements = new MutableList[Statement]()
  val returns = new MutableList[Identifier]()
  def addStatement(s: Statement) = statements += s
  def setParameters(params: List[Parameter]) = {
    parameters.clear()
    parameters ++= params
  }
  def setReturns(rets: List[Identifier]) = {
    returns.clear()
    returns ++= rets
  }
  def addInclude(p: Path) = includes += p
  def reset() = {
    includes.clear()
    parameters.clear()
    statements.clear()
    returns.clear()
  }
  def getProgram(name: String) : Program = {
    val id = Identifier(name)
    val cons = Construction(id,parameters.toList, statements.toList, returns.toList)
    Program(includes.toList, List(cons))
  }
}

object ConstructREPL extends EvalLoop with App {
  override def prompt = "Construct $ "
  val ui = new UI
  ui.visible = true
  // Default output file
  var outputFile = "out.png"

  val programStore = new ProgramStore
  var interpreter = new ConstructInterpreter
  var first = true
  var suggestions = List[(List[Drawable],Expr,String)]()
  loop { line =>
    try {
      var redraw = true
      if (line == ":reset" || line == ":r") {
        interpreter = new ConstructInterpreter
        programStore.reset()
        first = true
      }
      else if (
        ConstructParser.parseSuggestionTake(line) match {
          case ConstructParser.Success(_, _) => {
            println("Suggestion take triggered")
            true
          }
          case e: ConstructParser.NoSuccess  => false
        }
      ) {
        ConstructParser.parseSuggestionTake(line) match {
          case ConstructParser.Success((pattern, sug_id), _) => {
            val draw_expr = suggestions find {
              case (drawables, expr, name) => name == sug_id
            }
            draw_expr map {
              case (draw, expr, _) => {
                val statement = Statement(pattern, expr)
                interpreter.execute(statement)
                programStore.addStatement(statement)
              }
            }
            if (!draw_expr.isDefined) println(s"Suggestion $sug_id not found")
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else if ((line startsWith ":draw") || (line startsWith ":d")) {
        val splitLine = line.split(" +")
        if (splitLine.length > 1) outputFile = splitLine(1)
        PNG.dump(interpreter.get_drawables.toList, outputFile)
      }
      else if ((line startsWith ":write") || (line startsWith ":w")) {
        val splitLine = line.split(" +")
        if (splitLine.length == 3) {
          val outputFile = splitLine(2)
          val name = splitLine(1)
          val programStr = PrettyPrinter.print(programStore.getProgram(name))
          new PrintWriter(outputFile) { write(programStr); close }
        }
        else println(":write syntax not recognized")
      }
      else if ((line startsWith ":suggest") || (line startsWith ":s")) {
        val splitLine = line.split(" +")
        if (splitLine.length > 1) {
          suggestions = interpreter.query(Identifier(splitLine(1))).toList
          val tmp_drawables = suggestions flatMap {
            case (drawables, _, _) => drawables
          }
          ui.setImage(PNG.getTmp(interpreter.get_drawables.toList, tmp_drawables))
          redraw = false
        }
      }
      else if (line startsWith "include") {
        ConstructParser.parseInclude(line) match {
          case ConstructParser.Success(Path(p), _) => {
            val (item_map, cons) = Loader(p)
            interpreter.add_items(item_map.values.toList)
            programStore.addInclude(Path(p))
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else if (line startsWith "return") {
        ConstructParser.parseReturns(line) match {
          case ConstructParser.Success(returns, _) => {
            programStore.setReturns(returns)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else if (first) {
        first = false
        ConstructParser.parseGivens(line) match {
          case ConstructParser.Success(t, _) => {
            interpreter.inputs(t, None)
            programStore.setParameters(t)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else {
        ConstructParser.parseStatement(line) match {
          case ConstructParser.Success(t, _) => {
            interpreter.execute(t)
            programStore.addStatement(t)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      if (redraw) ui.setImage(PNG.get(interpreter.get_drawables.toList))
    }
    catch {
      case e: ConstructError => println(e)
    }
  }
  ui.dispose()
}


class ImagePanel extends Panel
{
  private var _bufferedImage: BufferedImage = null

  def setImage(value: BufferedImage) =
  {
    _bufferedImage = value
  }

  override def paintComponent(g:Graphics2D) =
  {
    if (null != _bufferedImage) g.drawImage(_bufferedImage, 0, 0, null)
  }
}

class UI extends MainFrame {
  title = "Construct"
  preferredSize = new Dimension(500, 500)
  private val image = new ImagePanel()
  contents = image
  def setImage(img: BufferedImage) = {
    image.setImage(img)
    repaint()
  }
}
