package construct.input

import scala.tools.nsc.EvalLoop
import construct.input.parser.ConstructParser
import construct.semantics.ConstructInterpreter
import construct.output.PNG

object ConstructREPL extends EvalLoop with App {
  override def prompt = "Statement> "

  var interpreter = new ConstructInterpreter
  var first = true
  loop { line =>
    if (first) {
      first = false
      ConstructParser.parseGivens(line) match {
        case ConstructParser.Success(t, _) => {
          interpreter.inputs(t, None)
          println(interpreter)
        }
        case e: ConstructParser.NoSuccess  => println(e)
      }
    }
    else if (line == "reset") {interpreter = new ConstructInterpreter}
    else if (line == "draw") {
      PNG.dump(interpreter.objects)
    }
    else {
      ConstructParser.parseStatement(line) match {
        case ConstructParser.Success(t, _) => {
          interpreter.execute(t)
          println(interpreter)
        }
        case e: ConstructParser.NoSuccess  => println(e)
      }
    }
    PNG.dump(interpreter.objects)
  }
}
