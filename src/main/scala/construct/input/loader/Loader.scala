package construct.input.loader

import construct.input.ast.{Construction, Identifier, Item}

import scala.collection.immutable.HashMap

trait Loader {
  var filenames: List[String]

  /**
    * Returns whether the file was loaded
    * @param filename the file to load
    * @return whether it could be found
    */
  def addFile(filename: String): Unit
  def load(filename: String): (HashMap[Identifier, Item], Option[Construction])
  def reload(): (HashMap[Identifier, Item], Option[Construction])
}
