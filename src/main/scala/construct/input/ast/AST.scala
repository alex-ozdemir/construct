package construct.input.ast

import construct.engine._

case class Path(val path: String)
case class Program(val references: List[Path],
                   val constructions: List[Item])

sealed abstract class Item
case class Construction(val name: Identifier,
                        val parameters: List[Identifier],
                        val statements: List[Statement],
                        val returns: List[Identifier]) extends Item
case class Shape(val con: Construction) extends Item
case class Identifier(val name: String)
case class Statement(val pattern: Pattern, val expr: Expr)

sealed abstract class Pattern
case class Destructor(val ty: Identifier, val contents: List[Pattern]) extends Pattern
case class Id(val id: Identifier) extends Pattern
case class Tuple(val contents: List[Pattern]) extends Pattern

sealed abstract class Expr
case class FnApp(val fn: Identifier, val args: List[Expr]) extends Expr
case class Exactly(val id: Identifier) extends Expr

sealed abstract class NamedObject
case class NamedCircle(val name: String, val center: NamedPoint, val edge: NamedPoint) extends NamedObject
case class NamedLine(val name: String, val p1: NamedPoint, val p2: NamedPoint) extends NamedObject
case class NamedPoint(val name: String, val point: Point) extends NamedObject
