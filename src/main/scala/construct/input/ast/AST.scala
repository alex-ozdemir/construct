package construct.input.ast

import construct.engine._

case class Path(val path: String)
case class Program(val references: List[Path],
                   val constructions: List[Construction])
case class Construction(val name: Identifier,
                        val parameters: List[Identifier],
                        val statements: List[Statement],
                        val returns: List[Identifier])
case class Identifier(val name: String)
case class Statement(val ids: List[Identifier], val fn: Identifier, val args: List[Identifier])


sealed abstract class NamedObject
case class NamedCircle(val name: String, val center: NamedPoint, val edge: NamedPoint) extends NamedObject
case class NamedLine(val name: String, val p1: NamedPoint, val p2: NamedPoint) extends NamedObject
case class NamedPoint(val name: String, val point: Point) extends NamedObject
