package construct.input

import scala.tools.nsc.EvalLoop
import construct.input.parser.ConstructParser
import construct.semantics.ConstructInterpreter

object ConstructREPL extends EvalLoop with App {
  override def prompt = "Statement> "

  var interpreter = new ConstructInterpreter

  loop { line =>
    if (line == "reset") {interpreter = new ConstructInterpreter}
    else {
      ConstructParser.parseStatement(line) match {
        case ConstructParser.Success(t, _) => {
          interpreter(t)
          println(interpreter)
        }
        case e: ConstructParser.NoSuccess  => println(e)
      }
    }
  }
}
