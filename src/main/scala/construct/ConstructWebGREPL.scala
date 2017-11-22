package construct

import construct.engine.Point
import construct.input.ast.Program
import construct.input.loader.{Loader, NonLoader}
import construct.output.{CanvasDrawer, Drawable}
import construct.semantics.ConstructError
import org.scalajs.dom
import dom.document
import org.scalajs.dom.html.Canvas

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("ConstructWebGREPL")
class ConstructWebGREPL(printer: js.Function1[String,Unit]) extends GREPLFrontend {

  val loader: Loader = new NonLoader
  val backend: GREPLBackend = new ConstructGREPL(this, loader)
  var pixelsToPoints: Point => Point = { _ => Point(0,0)}

  @JSExport("printToShell")
  override def printToShell(msg: String): Unit = {
    printer(msg)
  }

  @JSExport("handleLine")
  def handleLine(line: String): Unit = {
    try {
      backend.processLine(line)
    } catch {
      case e: ConstructError => printToShell(s"Error: ${e.msg}")
    }
  }

  @JSExport("handleClick")
  def handleClick(x: Int, y: Int): Unit = {
    backend.processPointClick(pixelsToPoints(Point(x, y)))
  }

  override def drawToScreen(shapes: List[Drawable], suggestions: List[List[Drawable]]): Unit = {
    val drawer = new CanvasDrawer(document.getElementById("drawing").asInstanceOf[Canvas], shapes, suggestions)
    pixelsToPoints = drawer.draw()
  }


  /**
    * Write this program to this file. Return whether the write was successful
    *
    * @param program  the program to write
    * @param filename the file to save it in
    * @return false
    */
  override def write(program: Program, filename: String): Boolean = {
    printer(":write is not yer supported on web")
    false
  }

  override def draw(filename: String, drawables: List[Drawable]): Unit = {
    printer(":draw is not yer supported on web")
  }
}

object ConstructWebGREPL {


  def main(args: Array[String]): Unit = {
    println("Hello world! Welcome to Construct!")
  }
}
