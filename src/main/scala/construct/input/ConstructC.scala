package construct.input

import construct.input.parser.ConstructParser
import construct.semantics.ConstructInterpreter
import construct.output.TkzEuclide

object ConstructC extends App {
  var interpreter = new ConstructInterpreter
  val filename = if (args.length > 0) {args(0)} else {"example.con"}
  val program = io.Source.fromFile(filename).getLines.reduceLeft(_+"\n"+_)
  ConstructParser(program) match {
    case ConstructParser.Success(t, _) => {
      t.statements foreach {interpreter(_)}
      println(TkzEuclide.dump(t, interpreter.points, interpreter.loci))
    }
    case e: ConstructParser.NoSuccess  => println(e)
  }
}
