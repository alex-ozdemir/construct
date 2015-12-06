// Alex Ozdemir <aozdemir@hmc.edu>
// December 2015
//
// This file holds a whole-file processor for the Construct Language
// It should be used as such:
//
// ```
// sbt 'runMain construct.ConstructC my_file.con'
// ```
//
// Which will cause it to load my_file.con (and all dependencies) and then
// run the first construction in the file by giving it up to 3 points as its
// actual parameters.
//
// It will output the results of that construction to standard output as a
// LaTeX document, and will create `my_file.png` with a PNG rendering of the
// construction

package construct

import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.ConstructInterpreter
import construct.output.{TkzEuclide,PNG}

object ConstructC extends App {
  var interpreter = new ConstructInterpreter
  val filename = if (args.length > 0) {args(0)}
                 else {throw new Error("Must provide file to interpret")}
  val (constructions, main_option) = Loader(filename)
  val main = main_option getOrElse { throw new Error(s"No main constrution in $filename") }
  interpreter.run(main, constructions.values.toList)
  println(TkzEuclide.render(interpreter.get_drawables))
  println(PNG.dump(interpreter.get_drawables.toList, filename + ".png"))
}
