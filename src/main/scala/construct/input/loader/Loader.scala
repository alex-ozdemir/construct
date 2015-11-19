package construct.input.loader

import java.nio.file.{Path,Paths,Files}

import scala.io
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import construct.input.ast
import construct.input.ast.{Construction,Identifier,Program}
import construct.input.parser.ConstructParser

object Loader {

  def apply(filename: String) : (HashMap[Identifier,Construction],Option[Construction])= {
    var first : Option[Construction] = None
    val loaded = new HashSet[Path]()
    val constructions = new HashMap[Identifier,Construction]()
    val to_load: Queue[Path] = new Queue() :+ Paths.get(filename)
    while (!to_load.isEmpty) {
      val path = to_load.dequeue
      val dir = if (path.getParent == null) Paths.get(".") else path.getParent
      loaded += path
      val program = io.Source.fromFile(path.toString).getLines.reduceLeft(_+"\n"+_)
      ConstructParser(program) match {
        case ConstructParser.Success(p, _) => {
          val Program(imports, cons) = p
          if (first.isEmpty && !cons.isEmpty) first = Some(cons(0))
          println(s"Going to load $imports, from dir $dir")
          val importPaths = imports map {case ast.Path(pathStr) => dir.resolve(pathStr)}
          to_load ++= importPaths filter {path => !(loaded exists {Files.isSameFile(path,_)})}
          cons filter {constructions contains _.name} foreach {con =>
            throw new Error(s"Tried to load construction ${con.name} from file $path, but" +
              " a construction by that name already exists")
          }
          constructions ++= cons filter {con =>
            !(constructions contains con.name)} map {con =>
            (con.name, con)}
        }
        case e: ConstructParser.NoSuccess => {
          println(e)
          sys.exit(0)
        }
      }
    }
    (constructions, first)
  }

}
