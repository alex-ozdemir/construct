// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the loader for Construct
// It is responsible for recursively following dependencies and yielding
//  the constructions

package construct.input.loader

import java.nio.file.{Path,Paths,Files}

import scala.io
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import construct.input.ast
import construct.input.ast.{Item,Shape,Construction,Identifier,Program}
import construct.input.parser.ConstructParser

object Loader {

  def name(i: Item) : Identifier =
    i match {
      case c: Construction => c.name
      case Shape(c)        => c.name
    }

  def apply(filename: String) : (HashMap[Identifier,Item],Option[Construction])= {
    var first : Option[Construction] = None
    val loaded = new HashSet[Path]()
    val items = new HashMap[Identifier,Item]()
    val to_load: Queue[Path] = new Queue() :+ Paths.get(filename)
    while (!to_load.isEmpty) {
      val path = to_load.dequeue
      val dir = if (path.getParent == null) Paths.get(".") else path.getParent
      loaded += path
      val program = io.Source.fromFile(path.toString).getLines.reduceLeft(_+"\n"+_)
      ConstructParser(program) match {
        case ConstructParser.Success(p, _) => {
          val Program(imports, these_items) = p
          val cons = these_items collect {case c: Construction => c}
          if (first.isEmpty && !cons.isEmpty) first = Some(cons(0))
          val importPaths = imports map {case ast.Path(pathStr) => dir.resolve(pathStr)}
          to_load ++= importPaths filter {path => !(loaded exists {Files.isSameFile(path,_)})}
          /*these_items filter {items contains name(_)} foreach {i =>
            throw new Error(s"Tried to load construction ${name(i)} from file $path, but" +
              " a construction by that name already exists")
          }*/
          items ++= these_items filter {i =>
            !(items contains name(i))} map { i =>
              (name(i), i)
            }
        }
        case e: ConstructParser.NoSuccess => {
          println(e)
          sys.exit(0)
        }
      }
    }
    (items, first)
  }

}
