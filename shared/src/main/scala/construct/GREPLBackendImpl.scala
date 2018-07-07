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

import construct.input.ast._
import construct.input.loader.Loader
import construct.input.parser.ConstructParser
import construct.output.Drawable
import construct.semantics.ConstructError

class GREPLBackendImpl(val frontend: GREPLFrontend, val loader: Loader)
    extends GREPLBackend {
  // Default output file
  var outputFile = "out.png"

  // Initialize semantic systems
  val programStore = new ProgramStore(loader)
  var suggestions: List[(List[Drawable], Expr, String)] = List()

  var nextLetter = 'A'
  var clearSuggestions = false

  val helpMessage: String =
    """Metacommands:
  :h[elp]                           Print this message.
  :r[eset]                          Empty the canvas and reload base libraries
  :d[raw] [<file>]                  Draw canvas to `file`.
                                      Optionally set output file to `file`.
                                      Otherwise uses last file or "out.png"
  :u[ndo]                           Undo last action.
  :?                                Print interpreter state (for developers)
  :w[rite] <construction> [<file>]  Name the session `construction` and write to `file`.
                                      If `file` is missing, writes to "out.con"
  :s[uggest] [<construction>]       Suggest how `construction` might be used,
                                      or clear suggestions."""

  def drawableSuggestions: List[List[Drawable]] = suggestions map {
    case (drawables, _, _) => drawables
  }

  drawToUI()

  def printHelp(): Unit = {
    frontend.printToShell(helpMessage)
  }

  // Processes a metacommand
  def processMetaCommand(command: String): Unit =
    if (command == "reset" || command == "r") reset()
    else if ((command startsWith "draw") || (command startsWith "d")) {
      val splitCommand = command.split(" +")
      if (splitCommand.length > 1) outputFile = splitCommand(1)
      frontend.draw(outputFile, programStore.interpreter.get_drawables.toList)
    } else if ((command startsWith "undo") || (command startsWith "u"))
      programStore.undo()
    else if (command startsWith "?")
      frontend.printToShell(s"${programStore.interpreter}")
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
    if (splitCommand.length == 2 || splitCommand.length == 3) {
      val outputFile =
        if (splitCommand.length == 3) splitCommand(2) else "out.con"
      val name = splitCommand(1)
      programStore.getProgram(name) match {
        case Left(error) => frontend.printToShell(f"Error: $error")
        case Right(pgm)  => frontend.write(pgm, outputFile)
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
        case (pattern, sug_id) =>
          val draw_expr = suggestions find {
            case (_, _, name) => name == sug_id
          }
          suggestions = suggestions.filter {
            case (_, _, name) => name != sug_id
          }
          draw_expr foreach {
            case (_, expr, _) =>
              programStore.addStatement(Statement(pattern, expr))
          }
          if (draw_expr.isEmpty)
            frontend.printToShell(s"Suggestion $sug_id not found")
          clearSuggestions = false
      }
    }

  def processGreplInstruction(line: String): Unit =
    printIfParseError {
      ConstructParser.parseGREPLInstruction(line) map {
        case Include(Path(s)) =>
          try {
            programStore.addInclude(s)
          } catch {
            case e: ConstructError => frontend.printToShell(e.fullMsg)
          }
        case Returns(returns)            => programStore.setReturns(returns)
        case Givens(givens)              => programStore.setParameters(givens)
        case statement @ Statement(_, _) => programStore.addStatement(statement)
      }
    }

  def drawToUI(): Unit = {
    frontend.drawToScreen(programStore.interpreter.get_drawables.toList,
                          drawableSuggestions)
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

    do {
      potentialIdent = Identifier(nextLetter.toString)
      nextLetter = (nextLetter + 1).toChar
    } while (programStore.interpreter.vars.contains(potentialIdent))

    programStore.addPoint(potentialIdent, location)
    drawToUI()
  }
}
