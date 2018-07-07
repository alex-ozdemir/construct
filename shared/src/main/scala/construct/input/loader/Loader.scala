package construct.input.loader

import construct.input.ast.{Construction, Identifier, Item}

import scala.collection.immutable.HashMap

trait Loader {
  /**
    * Load a construct file at a particular path
    * @param filename the path
    * @return The list of bound items, with a first construction if present
    */
  def load(filename: String): (HashMap[Identifier, Item], Option[Construction])

  /**
    * Initialize the loader, potentially returning base items
    */
  def init(): HashMap[Identifier, Item]
}
