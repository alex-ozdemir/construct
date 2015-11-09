package construct.input

import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.ConstructInterpreter
import construct.output.TkzEuclide

object ConstructC extends App {
  var interpreter = new ConstructInterpreter
  val filename = if (args.length > 0) {args(0)} else {"example.con"}
  val (constructions, main_option) = Loader(filename)
  val main = main_option getOrElse { throw new Error(s"No main constrution in $filename") }
  interpreter.run(main, constructions.values.toList)
  println(TkzEuclide.dump(interpreter.objects))
}
