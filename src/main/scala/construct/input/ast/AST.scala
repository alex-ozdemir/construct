package construct.input.ast

import construct.engine._

case class Parameters(val parameters: List[Identifier])
case class Returns(val returns: List[Identifier])
case class PointsIntersectionStatement(val o1: Identifier,
                                       val o2: Identifier,
                                       val points: List[Identifier]) extends Statement
case class Path(val path: String)
case class Program(val references: List[Path],
                   val main: Option[Construction])
case class Construction(val name: Identifier,
                        val parameters: Parameters,
                        val statements: Seq[Statement],
                        val returns: Returns)
case class Identifier(val name: String)
sealed abstract class Statement
sealed abstract class Constructor extends Statement
sealed abstract class LocusConstructor extends Constructor
case class PointConstructor() extends Constructor
case class CircleConstructor(val center: Identifier, val edge: Identifier) extends LocusConstructor
case class LineConstructor(val p1: Identifier, val p2: Identifier) extends LocusConstructor
case class LetStatement(val id: Identifier, val obj: Constructor) extends Statement


sealed abstract class NamedObject
case class NamedCircle(val name: String, val center: NamedPoint, val edge: NamedPoint) extends NamedObject
case class NamedLine(val name: String, val p1: NamedPoint, val p2: NamedPoint) extends NamedObject
case class NamedPoint(val name: String, val point: Point) extends NamedObject
