package construct.input.loader

import scala.io
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import construct.input.ast._
import construct.input.parser.ConstructParser

object Loader {

  def apply(filename: String) : (HashMap[Identifier,Construction],Option[Construction])= {
    var first : Option[Construction] = None
    val loaded = new HashSet[Path]()
    val constructions = new HashMap[Identifier,Construction]()
    val to_load: Queue[Path] = new Queue() :+ Path(filename)
    while (!to_load.isEmpty) {
      val Path(file) = to_load.dequeue
      loaded += Path(file)
      val program = io.Source.fromFile(file).getLines.reduceLeft(_+"\n"+_)
      ConstructParser(program) match {
        case ConstructParser.Success(p, _) => {
          val Program(imports, cons) = p
          if (first.isEmpty && !cons.isEmpty) first = Some(cons(0))
          to_load ++= imports filter {file => !(loaded contains file)}
          cons filter {constructions contains _.name} foreach {con =>
            throw new Error(s"Tried to load construction ${con.name} from file $file, but" +
              " a construction by that name already exists")
          }
          constructions ++= cons filter {con =>
            !(constructions contains con.name)} map {con =>
            (con.name, con)}
        }
        case e: ConstructParser.NoSuccess => throw new Error(e.toString)
      }
    }
    (constructions, first)
  }

}
