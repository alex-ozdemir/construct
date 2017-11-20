// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This file holds the input AST for the construct system

package construct.input.ast

import construct.engine._
import scala.util.parsing.input.Positional

case class Path(val path: String)
case class Program(val references: List[Path], val constructions: List[Item])

sealed abstract class Item
case class Construction(val name: Identifier,
                        val parameters: List[Parameter],
                        val statements: List[Statement],
                        val returns: List[Identifier])
    extends Item
case class Shape(val con: Construction) extends Item

sealed abstract trait GREPLInstruction

case class Include(val path: Path) extends GREPLInstruction
case class Givens(val parameters: List[Parameter]) extends GREPLInstruction
case class Returns(val ids: List[Identifier]) extends GREPLInstruction

// let <Pattern> = <Expr>
case class Statement(val pattern: Pattern, val expr: Expr)
    extends GREPLInstruction

sealed abstract class Pattern extends Positional {
  def boundIdents: Set[Identifier]
}
case class Destructor(val ty: Identifier, val contents: List[Pattern])
    extends Pattern {
  def boundIdents: Set[Identifier] = (contents flatMap { _.boundIdents }).toSet
}
case class Id(val id: Identifier) extends Pattern {
  def boundIdents: Set[Identifier] = Set(id)
}
case class Tuple(val contents: List[Pattern]) extends Pattern {
  def boundIdents: Set[Identifier] = (contents flatMap { _.boundIdents }).toSet
}

sealed abstract class Expr extends Positional {
  def usedIdents: Set[Identifier]
}
case class FnApp(val fn: Identifier, val args: List[Expr]) extends Expr {
  def usedIdents: Set[Identifier] = (args flatMap { _.usedIdents }).toSet
}
case class Difference(val left: Expr, val right: Expr) extends Expr {
  def usedIdents: Set[Identifier] = (left.usedIdents | right.usedIdents).toSet
}
case class SetLit(val items: List[Expr]) extends Expr {
  def usedIdents: Set[Identifier] = (items flatMap { _.usedIdents }).toSet
}
case class Exactly(val id: Identifier) extends Expr {
  def usedIdents: Set[Identifier] = Set(id)
}

case class Identifier(val name: String)
case class Parameter(val name: Identifier, val ty: Identifier)
    extends Positional
