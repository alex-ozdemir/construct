// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the loader for Construct
// It is responsible for recursively following dependencies and yielding
//  the constructions

package construct.input.loader

import java.io.FileNotFoundException
import java.nio.file.{Files, Path, Paths}

import scala.collection.mutable.Queue
import scala.collection.mutable.HashSet
import construct.input.ast
import construct.input.ast._
import construct.input.parser.ConstructParser
import construct.semantics.ConstructError

import scala.collection.{immutable, mutable}
import scala.collection.immutable.HashMap

class FileSystemLoader() extends Loader {


  override def init(): HashMap[Identifier, Item] = HashMap()

  override def load(filename: String): (HashMap[Identifier, Item], Option[Construction]) = {
    loadFrom(List(filename))
  }

  private def loadFrom(files: List[String]) = {
    try {
      var first: Option[Construction] = None
      val loaded = new mutable.HashSet[Path]()
      var items = immutable.HashMap[Identifier, Item]()
      val to_load: mutable.Queue[Path] = new mutable.Queue()
      to_load ++= files map {
        Paths.get(_)
      }
      while (to_load.nonEmpty) {
        val path = to_load.dequeue
        val dir = if (path.getParent == null) Paths.get(".") else path.getParent
        loaded += path
        val program =
          io.Source.fromFile(path.toString).getLines.reduceLeft(_ + "\n" + _)
        ConstructParser(program) match {
          case ConstructParser.Success(p, _) => {
            val Program(imports, these_items) = p
            val cons = these_items collect { case c: Construction => c }
            if (first.isEmpty && cons.nonEmpty) first = Some(cons(0))
            val importPaths = imports map {
              case ast.Path(pathStr) => dir.resolve(pathStr)
            }
            to_load ++= importPaths filter { path =>
              !(loaded exists {
                Files.isSameFile(path, _)
              })
            }
            items ++= these_items filter { i =>
              !(items contains i.name)
            } map { i =>
              (i.name, i)
            }
          }
          case e: ConstructParser.NoSuccess => {
            throw ConstructError.IncludeError(path.toString, s"parser error: ${e.msg}\n${e.next.pos.longString}")
          }
        }
      }
      (items, first)
    } catch {
      case e: FileNotFoundException => throw ConstructError.FileNotFound(e.getMessage)
    }
  }
}
