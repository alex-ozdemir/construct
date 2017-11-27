package construct.input.loader
import construct.input.ast.{Construction, Identifier, Item}
import construct.input.parser.ConstructParser
import construct.semantics.ConstructError.{IncludeError, WebInclude}

import scala.collection.immutable.HashMap

import scala.scalajs.js

class WebLoader(reader: js.Function0[String]) extends Loader {

  override def load(filename: String): (HashMap[Identifier, Item], Option[Construction]) =
    throw WebInclude(filename)

  override def init(): (HashMap[Identifier, Item]) = {
    ConstructParser(reader()) match {
      case ConstructParser.Success(p, _) =>
        if (p.references.nonEmpty) {
          throw WebInclude(p.references.head.path)
        } else {
          var m: HashMap[Identifier, Item] = HashMap()
          m ++= p.constructions map { i: Item => (i.name, i) }
          m
        }
      case e: ConstructParser.NoSuccess =>
        throw IncludeError("library", s"parser error: ${e.msg}\n${e.next.pos.longString}")
    }
  }
}
