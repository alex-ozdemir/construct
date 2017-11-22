package construct.input.loader
import construct.input.ast.{Construction, Identifier, Item}

import scala.collection.immutable.HashMap

class NonLoader extends Loader {
  override var filenames: List[String] = List()

  override def addFile(filename: String): Unit = {}
  override def load(filename: String): (HashMap[Identifier, Item], Option[Construction]) = (HashMap(), None)

  override def reload(): (HashMap[Identifier, Item], Option[Construction]) = (HashMap(), None)
}
