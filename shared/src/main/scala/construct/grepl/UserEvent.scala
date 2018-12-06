package construct.grepl

import construct.engine

sealed abstract class UserEvent

case class LineEntered(string: String) extends UserEvent

case class PointAdded(pt: engine.Point) extends UserEvent