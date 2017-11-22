// Alex Ozdemir <aozdemir@hmc.edu>
// December 2015
//
// This file holds the interactive system (The Graphical Read Evaluate Print Loop)
// for the Construct language.
// It should be used as such:
//
// ```
// sbt 'runMain construct.ConstructGREPL'
// Construct $ // Click on the diagram in two locations to gets points A, B
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

import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.{ConstructError, ConstructInterpreter}
import construct.output.{Drawable, PNG, PrettyPrinter}

import scala.collection.mutable

abstract class UndoableAction(val indentifiers: Traversable[Identifier])
object UndoableAction {
  // The identifier produced by adding this point
  case class AddPoint(identifier: Identifier) extends UndoableAction(List(identifier))
  // The identifiers produced by this statement
  case class Statement(identifiers: Traversable[Identifier]) extends UndoableAction(identifiers)
}

// A Program Store tracks the statements that the user has done so far
// This information can be used to extract an equivalent Construct program
// or to undo a step
//
// It also maintains an up-to-date interpreter with the current program
class ProgramStore(val loader: Loader) {
  val includes = new mutable.MutableList[Path]()
  val parameters = new mutable.MutableList[Parameter]()
  var statements = new mutable.MutableList[Statement]()
  val returns = new mutable.MutableList[Identifier]()
  val undoableActions = new mutable.ArrayStack[UndoableAction]()
  var interpreter = new ConstructInterpreter
  interpreter.add_items(loader.init().values)

  def addStatement(s: Statement): Unit = {
    undoableActions += UndoableAction.Statement(interpreter.execute(s))
    statements += s
  }
  def undo(): Unit = {
    if (undoableActions.nonEmpty) {
      undoableActions.pop() match {
        case UndoableAction.Statement(ids) =>
          statements = statements dropRight 1
          ids foreach { interpreter.vars.remove(_) }
        case UndoableAction.AddPoint(id) =>
          interpreter.vars.remove(id)
      }
    }
  }

  def addPoint(name: Identifier, pt: engine.Point): Unit = {
    interpreter.add_input(Parameter(name, Identifier("point")),
                          Some(semantics.Basic(pt)))
    undoableActions += UndoableAction.AddPoint(name)
  }
  def setParameters(params: List[Parameter]): Unit = {
    parameters.clear()
    parameters ++= params
  }
  def setReturns(rets: List[Identifier]): Unit = {
    returns.clear()
    returns ++= rets
  }
  def addInclude(p: String): Unit = {
    val (item_map, cons) = loader.load(p)
    interpreter.add_items(item_map.values.toList)
    includes += Path(p)
  }
  def reset(): Unit = {
    includes.clear()
    parameters.clear()
    statements.clear()
    returns.clear()
    interpreter = new ConstructInterpreter
    interpreter.add_items(loader.init().values)
  }

  /**
    * Tries to extract a program. Might error out because
    *  * There are no stated returns
    *  * There are dependencies on undeclared parameters
    */
  def getProgram(name: String): Either[String, Program] = {
    if (returns.isEmpty) return Left("There are no returns")

    // We verify that the returns depend only on the declared parameters
    val paramIdents = (parameters map { _.name }).toSet
    var liveIds = returns.toSet &~ paramIdents
    var neededStatements: List[Statement] = List()
    statements.reverseIterator foreach {
      case s @ Statement(pattern, expression) => {
        if ((liveIds & pattern.boundIdents).nonEmpty) {
          liveIds = ((liveIds &~ pattern.boundIdents) | expression.usedIdents) &~ paramIdents
          neededStatements = s :: neededStatements
        }
      }
    }
    if (liveIds.nonEmpty) {
      val sing_return = returns.size == 1
      val sing_live = liveIds.size == 1
      Left(f"The return${if (sing_return) "" else "s"}, ${PrettyPrinter
        .printIds(returns)}, ${if (sing_return) "is" else "are"} dependent on ${PrettyPrinter
        .printIds(liveIds)}, which ${if (sing_live) "is" else "are"} not given")
    } else {
      val id = Identifier(name)
      val cons =
        Construction(id, parameters.toList, neededStatements, returns.toList)
      Right(Program(includes.toList, List(cons)))
    }
  }
}

trait GREPLFrontend {
  def printToShell(msg: String): Unit
  def drawToScreen(shapes: List[Drawable], suggestions: List[List[Drawable]]): Unit
  def draw(filename: String, drawables: List[Drawable])

  /**
    * Write this program to this file. Return whether the write was successful
    * @param program the program to write
    * @param filename the file to save it in
    * @return
    */
  def write(program: Program, filename: String): Boolean
}

trait GREPLBackend {
  def processLine(line: String): Unit
  def processPointClick(pt: engine.Point): Unit
}

class ConstructGREPL(val frontend: GREPLFrontend, val loader: Loader) extends GREPLBackend {
  // Default output file
  var outputFile = "out.png"

  // Initialize semantic systems
  val programStore = new ProgramStore(loader)
  var suggestions: List[(List[Drawable], Expr, String)] = List()

  var nextLetter = 'A'
  var clearSuggestions = false

  def drawableSuggestions: List[List[Drawable]] = suggestions map {
    case (drawables, _, _) => drawables.toList
  }

  drawToUI()

  def printHelp(): Unit = {
    val message =
      """Metacommands:
  :h[elp]                         Print this message.
  :r[eset]                        Empty the canvas and reload base libraries
  :d[raw] [<file>]                Draw canvas to `file`.
                                    Optionally set output file to `file`.
                                    Otherwise uses last file or "out.png"
  :u[ndo]                         Undo last action.
  :?                              Print interpreter state (for developers)
  :w[rite] <construction> <file>  Name the session `construction` and write to `file`.
  :s[uggest] [<construction>]     Suggest how `construction` might be used,
                                    or clear suggestions."""
    frontend.printToShell(message)
  }

  // Processes a metacommand
  def processMetaCommand(command: String): Unit =
    if (command == "reset" || command == "r") reset()
    else if ((command startsWith "draw") || (command startsWith "d")) {
      val splitCommand = command.split(" +")
      if (splitCommand.length > 1) outputFile = splitCommand(1)
      frontend.draw(outputFile, programStore.interpreter.get_drawables.toList)
    } else if ((command startsWith "undo") || (command startsWith "u")) programStore.undo()
    else if (command startsWith "?") frontend.printToShell(s"${programStore.interpreter}")
    else if ((command startsWith "write") || (command startsWith "w"))
      write(command)
    else if ((command startsWith "suggest") || (command startsWith "s"))
      suggest(command)
    else if ((command startsWith "help") || (command startsWith "h"))
      printHelp()
    else frontend.printToShell(s"Unrecognized metacommand :$command")

  // ============================================= //
  // Helper functions for processing meta-commands //
  // ============================================= //

  def reset(): Unit = {
    nextLetter = 'A'
    programStore.reset()
  }

  def write(command: String): Unit = {
    val splitCommand = command.split(" +")
    if (splitCommand.length == 3) {
      val outputFile = splitCommand(2)
      val name = splitCommand(1)
      programStore.getProgram(name) match {
        case Left(error) => frontend.printToShell(f"Error: $error")
        case Right(pgm) => frontend.write(pgm, outputFile)
      }
    } else frontend.printToShell(":write syntax not recognized")
  }

  def suggest(command: String): Unit = {
    val splitCommand = command.split(" +")
    if (splitCommand.length > 1) {
      suggestions =
        programStore.interpreter.query(Identifier(splitCommand(1))).toList
      clearSuggestions = false
    }
  }

  // ================================================== //
  // Helper functions for processing Construct language //
  // ================================================== //

  def printIfParseError[T](result: ConstructParser.ParseResult[T]): Unit =
    if (!result.successful) frontend.printToShell(s"$result")

  def takeSuggestion(line: String): Unit =
    printIfParseError {
      ConstructParser.parseSuggestionTake(line) map {
        case (pattern, sug_id) => {
          val draw_expr = suggestions find {
            case (_, _, name) => name == sug_id
          }
          suggestions = suggestions.filter {
            case (_, _, name) => name != sug_id
          }
          draw_expr foreach {
            case (draw, expr, _) =>
              programStore.addStatement(Statement(pattern, expr))
          }
          if (draw_expr.isEmpty) frontend.printToShell(s"Suggestion $sug_id not found")
          clearSuggestions = false
        }
      }
    }

  def processGreplInstruction(line: String): Unit =
    printIfParseError {
      ConstructParser.parseGREPLInstruction(line) map {
        case Include(Path(s)) => {
          try {
            programStore.addInclude(s)
          } catch {
            case e: ConstructError => frontend.printToShell(f"$e")
          }
        }
        case Returns(returns)            => programStore.setReturns(returns)
        case Givens(givens)              => programStore.setParameters(givens)
        case statement @ Statement(_, _) => programStore.addStatement(statement)
      }
    }

  def drawToUI(): Unit = {
    frontend.drawToScreen(programStore.interpreter.get_drawables.toList, drawableSuggestions)
  }

  def processLine(line: String): Unit = {
    clearSuggestions = true
    if (line startsWith ":") processMetaCommand(line.substring(1))
    else if (ConstructParser.parseSuggestionTake(line).successful)
      takeSuggestion(line)
    else processGreplInstruction(line)
    if (clearSuggestions) suggestions = List()
    drawToUI()
  }

  override def processPointClick(location: engine.Point): Unit = {
    var potentialIdent: Identifier = null
    val ty = Identifier("point")

    do {
      potentialIdent = Identifier(nextLetter.toString)
      nextLetter = (nextLetter + 1).toChar
    } while (programStore.interpreter.vars.contains(potentialIdent))

    programStore.addPoint(potentialIdent, location)
    drawToUI()
  }
}

