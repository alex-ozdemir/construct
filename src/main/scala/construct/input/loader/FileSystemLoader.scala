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
import construct.semantics.{FileNotFound, IncludeError}

import scala.collection.immutable
import scala.collection.immutable.HashMap

class FileSystemLoader() extends Loader {

  override var filenames: List[String] = List()

  def name(i: Item): Identifier =
    i match {
      case c: Construction => c.name
      case Shape(c)        => c.name
    }

  override def addFile(filename: String): Unit = {
    val program = io.Source.fromFile(filename).getLines.reduceLeft(_ + "\n" + _)
    filenames = filenames :+ filename
  }

  override def load(filename: String): (HashMap[Identifier, Item], Option[Construction]) = {
    val ret = loadFrom(List(filename))
    filenames = filenames :+ filename
    ret
  }

  override def reload(): (HashMap[Identifier, Item], Option[Construction]) = {
    loadFrom(filenames)
  }

  private def loadFrom(files: List[String]) = {
    try {
      var first: Option[Construction] = None
      val loaded = new HashSet[Path]()
      var items = immutable.HashMap[Identifier, Item]()
      val to_load: Queue[Path] = new Queue()
      to_load ++= files map {
        Paths.get(_)
      }
      while (!to_load.isEmpty) {
        val path = to_load.dequeue
        val dir = if (path.getParent == null) Paths.get(".") else path.getParent
        loaded += path
        val program =
          io.Source.fromFile(path.toString).getLines.reduceLeft(_ + "\n" + _)
        ConstructParser(program) match {
          case ConstructParser.Success(p, _) => {
            val Program(imports, these_items) = p
            val cons = these_items collect { case c: Construction => c }
            if (first.isEmpty && !cons.isEmpty) first = Some(cons(0))
            val importPaths = imports map {
              case ast.Path(pathStr) => dir.resolve(pathStr)
            }
            to_load ++= importPaths filter { path =>
              !(loaded exists {
                Files.isSameFile(path, _)
              })
            }
            /*these_items filter {items contains name(_)} foreach {i =>
              throw new Error(s"Tried to load construction ${name(i)} from file $path, but" +
                " a construction by that name already exists")
            }*/
            items ++= these_items filter { i =>
              !(items contains name(i))
            } map { i =>
              (name(i), i)
            }
          }
          case e: ConstructParser.NoSuccess => {
            throw IncludeError(path.toString, s"parser error: ${e.msg}\n${e.next.pos.longString}")
          }
        }
      }
      (items, first)
    } catch {
      case e: FileNotFoundException => throw FileNotFound(e.getMessage)
    }
  }
}
