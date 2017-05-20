// Alex Ozdemir <aozdemir@hmc.edu>
// December 2015
//
// This file holds the interactive system (The Graphical Read Evaluate Print Loop)
// for the Construct language.
// It should be used as such:
//
// ```
// sbt 'runMain construct.ConstructGREPL'
// Construct $ given point A, point B
// Construct $ let C1 = circle(A, B)
// Construct $ :s circle
//             // Circle suggestion is displayed
// Construct $ let C2 = 0
// Construct $ let D, E = intersection(C1, C2)
// Construct $ :draw out.png
//             // Construction is rendered as PNG to out.png
// Construct $ return D, E
// Construct $ :write triangle_pts tri.con
//             // Construciton is outputed as code under the name triangle_pts
//             // to the file tri.con
// Construct $ :reset
//             // Construction is reset (to blank)
// Construct $ given point A, point B
// Construct $ let L = line(A, B)
// Construct $ :undo
//             // Last statement is reverted
// ```

package construct

import java.awt.image.BufferedImage
import java.io.{FileWriter,PrintWriter}

import scala.tools.nsc.EvalLoop
import scala.swing._
import scala.collection.mutable.MutableList

import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.{ConstructInterpreter,ConstructError}
import construct.output.{Drawable,PNG,PrettyPrinter}
import construct.engine._

// A Program Store tracks the statements that the user has done so far
// This information can be used to extract an equivalent Construct program
// or to undo a step
class ProgramStore {
  val includes = new MutableList[Path]()
  val parameters = new MutableList[Parameter]()
  var statements = new MutableList[Statement]()
  val returns = new MutableList[Identifier]()
  def addStatement(s: Statement) = statements += s
  def undoStatement() = statements = statements dropRight 1
  def addParameter(param: Parameter) = {
    parameters += param
  }
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
    val cons = Construction(id, parameters.toList, statements.toList, returns.toList)
    Program(includes.toList, List(cons))
  }
}

object ConstructGREPL extends EvalLoop with App {
  override def prompt = "Construct $ "
  // Default output file
  var outputFile = "out.png"

  // Initialize semantic systems
  val programStore = new ProgramStore
  var interpreter = new ConstructInterpreter
  var suggestions = List[(List[Drawable],Expr,String)]()

  var nextLetter = 'A'

  // Initialize UI
  val ui = new UI(pt => {
    pixelsToPoints(pt) match {
      case Some(location) => {
        var potentialIdent: Identifier = null;
        val ty = Identifier("point")

        do {
          potentialIdent = Identifier(nextLetter.toString)
          nextLetter = (nextLetter + 1).toChar
        } while (interpreter.vars.contains(potentialIdent))

        val param = Parameter(potentialIdent, ty)
        addGiven(param, Some(semantics.Basic(location)))
        drawToUI()
      }
      case None => { }
    }
  })
  var pixelsToPoints: swing.Point => Option[engine.Point] = {
    pt => Some(Point(0.0, 0.0))
  }
  var redraw = true
  ui.visible = true
  drawToUI()

  def printHelp() = {
    val message = """Metacommands:
  :h[elp]                         Print this message.
  :r[eset]                        Empty the canvas.
  :d[raw]  [file]                 Draw canvas to `file`. Optionally set output file to `file`.
  :u[ndo]                         Undo last action.
  :?                              Print interpreter state.
  :w[rite] [construction] [file]  Name the session `construction` and write to `file`.
  :s[uggest] [construction]       Suggest how `construction` might be used."""
    println(message)
  }

  // Processes a metacommand
  def processMetaCommand(command: String) =
    if (command == "reset" || command == "r") reset()
    else if (command startsWith "dt") {
      val splitCommand = command.split(" +")
      if (splitCommand.length > 1) outputFile = splitCommand(1)
      drawTmp
    }
    else if ((command startsWith "draw") || (command startsWith "d")) {
      val splitCommand = command.split(" +")
      if (splitCommand.length > 1) outputFile = splitCommand(1)
      draw
    }
    else if ((command startsWith "undo") || (command startsWith "u")) undo()
    else if (command startsWith "?") println(interpreter)
    else if ((command startsWith "write") || (command startsWith "w")) write(command)
    else if ((command startsWith "suggest") || (command startsWith "s")) suggest(command)
    else if ((command startsWith "help") || (command startsWith "h")) printHelp()
    else println(s"Unrecognized metacommand :$command")

  // ============================================= //
  // Helper functions for processing meta-commands //
  // ============================================= //

  def reset() = {
    nextLetter = 'A'
    interpreter = new ConstructInterpreter
    programStore.reset()
  }

  def draw = PNG.dump(interpreter.get_drawables.toList, outputFile)

  def drawTmp = {
    println(suggestions)
    val tmp_drawables = suggestions map {
      case (drawables, _, _) => drawables.toList
    }
    PNG.dumpTmp(interpreter.get_drawables.toList, tmp_drawables, outputFile)
  }

  def undo() = {
    programStore.undoStatement()
    interpreter = new ConstructInterpreter
    interpreter.set_inputs(programStore.parameters, None)
    programStore.includes foreach {case Path(p) => {
      val (item_map, cons) = Loader(p)
      interpreter.add_items(item_map.values.toList)
    } }
    programStore.statements foreach { interpreter.execute(_) }
  }

  def write(command: String) = {
    val splitCommand = command.split(" +")
    if (splitCommand.length == 3) {
      val outputFile = splitCommand(2)
      val name = splitCommand(1)
      val programStr = PrettyPrinter.print(programStore.getProgram(name))
      new PrintWriter(new FileWriter(outputFile, true)) { write(programStr); close }
    }
    else println(":write syntax not recognized")
  }

  def suggest(command: String) = {
    val splitCommand = command.split(" +")
    if (splitCommand.length > 1) {
      suggestions = interpreter.query(Identifier(splitCommand(1))).toList
      val tmp_drawables = suggestions map {
        case (drawables, _, _) => drawables
      }
      ui.setImage(PNG.getTmp(interpreter.get_drawables.toList, tmp_drawables))
      redraw = false
    }
  }

  // ================================================== //
  // Helper functions for processing Construct language //
  // ================================================== //

  def printIfError[T](result: ConstructParser.ParseResult[T]) =
    if (!result.successful) println(result)

  def takeSuggestion(line: String) =
    printIfError {
      ConstructParser.parseSuggestionTake(line) map {
        case (pattern, sug_id) => {
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
      }
    }

  def processInclude(line: String) =
    printIfError {
      ConstructParser.parseInclude(line) map {
        case Path(p) => {
          try {
            val (item_map, cons) = Loader(p)
            interpreter.add_items(item_map.values.toList)
            programStore.addInclude(Path(p))
          } catch {
            case e : java.io.FileNotFoundException => println(e)
          }
        }
      }
    }

  def processReturn(line: String) =
    printIfError {
      ConstructParser.parseReturns(line) map {
        returns => programStore.setReturns(returns)
      }
    }

  def addGiven(given: Parameter, v: Option[semantics.Var]) = {
    interpreter.add_input(given, v)
    programStore.addParameter(given)
  }

  def processGiven(line: String) = {
    printIfError {
      ConstructParser.parseGivens(line) map {
        givens => givens foreach { param => addGiven(param, None) }
      }
    }
  }

  def processStatement(line: String) =
    printIfError {
      ConstructParser.parseStatement(line) map {
        statement => {
          interpreter.execute(statement)
          programStore.addStatement(statement)
        }
      }
    }

  def drawToUI(): Unit = {
    val (image, revereseHomography) = PNG.get(interpreter.get_drawables.toList)
    ui.setImage(image)
    pixelsToPoints = {
      pt => Some(revereseHomography(Point(pt.x, pt.y)))
    }
  }


  // =============== //
  // The actual loop //
  // =============== //

  loop { line =>
    try {
      redraw = true
      if (line startsWith ":") processMetaCommand(line.substring(1))
      else if (ConstructParser.parseSuggestionTake(line).successful) takeSuggestion(line)
      else if (line startsWith "include") processInclude(line)
      else if (line startsWith "return") processReturn(line)
      else if (line startsWith "given") processGiven(line)
      else processStatement(line)
      if (redraw) drawToUI()
    }
    catch {
      case e: ConstructError => println(e)
    }
  }

  // Teardown the UI
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

class UI(onclick: java.awt.Point => Unit) extends MainFrame {
  title = "Construct GREPL"
  preferredSize = new Dimension(500, 500)
  private val image = new ImagePanel()
  contents = image
  def setImage(img: BufferedImage) = {
    image.setImage(img)
    repaint()
  }
  listenTo(image.mouse.clicks)
  reactions += {
    case event.MouseClicked(_, p, _, _, _) => {
      onclick(p)
    }
  }
}
