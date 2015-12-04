// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This file holds the input AST for the construct system

package construct.input.ast

import construct.engine._

case class Path(val path: String)
case class Program(val references: List[Path],
                   val constructions: List[Item])

sealed abstract class Item
case class Construction(val name: Identifier,
                        val parameters: List[Parameter],
                        val statements: List[Statement],
                        val returns: List[Identifier]) extends Item
case class Shape(val con: Construction) extends Item

// let <Pattern> = <Expr>
case class Statement(val pattern: Pattern, val expr: Expr)

sealed abstract class Pattern
case class Destructor(val ty: Identifier, val contents: List[Pattern]) extends Pattern
case class Id(val id: Identifier) extends Pattern
case class Tuple(val contents: List[Pattern]) extends Pattern

sealed abstract class Expr
case class FnApp(val fn: Identifier, val args: List[Expr]) extends Expr
case class Exactly(val id: Identifier) extends Expr

case class Identifier(val name: String)
case class Parameter(val name: Identifier, val ty: Identifier)
