// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This file holds the input AST for the construct system

package construct.input.ast

import scala.util.parsing.input.Positional

case class Program(references: List[Path], constructions: List[Item])

case class Path(path: String)

sealed abstract class Item {
  val name: Identifier
}
case class Construction(name: Identifier,
                        parameters: List[Parameter],
                        statements: List[Statement],
                        returns: List[Identifier])
    extends Item
case class Shape(con: Construction) extends Item {
  val name: Identifier = con.name
}

sealed trait GREPLInstruction

case class Include(path: Path) extends GREPLInstruction
case class Givens(parameters: List[Parameter]) extends GREPLInstruction
case class Returns(ids: List[Identifier]) extends GREPLInstruction

// let <Pattern> = <Expr>
case class Statement(pattern: Pattern, expr: Expr)
    extends GREPLInstruction with Positional

sealed abstract class Pattern extends Positional {
  def boundIdents: Set[Identifier]
}
case class Destructor(ty: Identifier, contents: List[Pattern])
    extends Pattern {
  def boundIdents: Set[Identifier] = (contents flatMap { _.boundIdents }).toSet
}
case class Id(id: Identifier) extends Pattern {
  def boundIdents: Set[Identifier] = Set(id)
}
case class Tuple(contents: List[Pattern]) extends Pattern {
  def boundIdents: Set[Identifier] = (contents flatMap { _.boundIdents }).toSet
}

sealed abstract class Expr extends Positional {
  def usedIdents: Set[Identifier]
}
case class FnApp(fn: Identifier, args: List[Expr]) extends Expr {
  def usedIdents: Set[Identifier] = (args flatMap { _.usedIdents }).toSet
}
case class Difference(left: Expr, right: Expr) extends Expr {
  def usedIdents: Set[Identifier] = (left.usedIdents | right.usedIdents).toSet
}
case class SetLit(items: List[Expr]) extends Expr {
  def usedIdents: Set[Identifier] = (items flatMap { _.usedIdents }).toSet
}
case class Exactly(id: Identifier) extends Expr {
  def usedIdents: Set[Identifier] = Set(id)
}

case class Identifier(name: String) extends Positional
case class Parameter(name: Identifier, ty: Identifier)
    extends Positional
