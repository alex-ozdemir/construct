package construct.input.ast

case class IntersectionStatement(val o1: Identifier, val o2: Identifier, val points: List[Identifier]) extends Statement
case class Program(val statements: Seq[Statement])
case class Identifier(val name: String)
sealed abstract class Statement
sealed abstract class Constructor extends Statement
sealed abstract class LocusConstructor extends Constructor
case class PointConstructor() extends Constructor
case class CircleConstructor(val center: Identifier, val edge: Identifier) extends LocusConstructor
case class LineConstructor(val p1: Identifier, val p2: Identifier) extends LocusConstructor
case class LetStatement(val id: Identifier, val obj: Constructor) extends Statement
