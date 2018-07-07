package construct

import construct.input.ast._
import construct.input.loader.Loader
import construct.output.PrettyPrinter
import construct.semantics.ConstructInterpreter

import scala.collection.mutable

// A Program Store tracks the statements that the user has done so far
// This information can be used to extract an equivalent Construct program
// or to undo a step
//
// It also maintains an up-to-date interpreter with the current program
class ProgramStore(val loader: Loader) {

  private abstract class UndoableAction(val indentifiers: Traversable[Identifier])
  private object UndoableAction {
    // The identifier produced by adding this point
    case class AddPoint(identifier: Identifier) extends UndoableAction(List(identifier))
    // The identifiers produced by this statement
    case class Statement(identifiers: Traversable[Identifier]) extends UndoableAction(identifiers)
  }

  val includes = new mutable.MutableList[Path]()
  val parameters = new mutable.MutableList[Parameter]()
  var statements = new mutable.MutableList[Statement]()
  val returns = new mutable.MutableList[Identifier]()
  private val undoableActions = new mutable.ArrayStack[UndoableAction]()
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
                          Some(semantics.Value.Basic(pt)))
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
    val (item_map, _) = loader.load(p)
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
      case s @ Statement(pattern, expression) =>
        if ((liveIds & pattern.boundIdents).nonEmpty) {
          liveIds = ((liveIds &~ pattern.boundIdents) | expression.usedIdents) &~ paramIdents
          neededStatements = s :: neededStatements
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
