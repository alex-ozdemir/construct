package construct

import construct.engine.Point
import construct.grepl._
import construct.input.ast.Program
import construct.input.loader.{Loader, WebLoader}
import construct.output.{CanvasDrawer, Drawable, PrettyPrinter}
import construct.semantics.ConstructError
import org.scalajs.dom.document
import org.scalajs.dom.html.Canvas

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("ConstructWebGREPL")
class ConstructWebGREPL(printer: js.Function1[String, Unit],
                        writer: js.Function1[String, Unit],
                        reader: js.Function0[String])
    extends GREPLFrontend {

  val loader: Loader = new WebLoader(reader)
  val backend: GREPLBackend = new GREPLBackendImpl(this, loader)

  var pixelsToPoints: Point => Point = { _ =>
    Point(0, 0)
  }

  @JSExport("printToShell")
  override def printToShell(msg: String): Unit = {
    printer(msg)
  }

  @JSExport("handleLine")
  def handleLine(line: String): Unit = {
    try {
      backend.processEvent(LineEntered(line))
    } catch {
      case e: ConstructError => printToShell(e.fullMsg)
    }
  }

  @JSExport("handleClick")
  def handleClick(x: Int, y: Int): Unit = {
    backend.processEvent(PointAdded(pixelsToPoints(Point(x, y))))
  }

  override def drawToScreen(shapes: List[Drawable],
                            suggestions: List[List[Drawable]]): Unit = {
    val drawer = new CanvasDrawer(
      document.getElementById("drawing").asInstanceOf[Canvas],
      shapes,
      suggestions)
    pixelsToPoints = drawer.draw()
  }

  /**
    * Write this program to this file. Return whether the write was successful
    *
    * @param program  the program to write
    * @param filename the file to save it in
    * @return true
    */
  override def write(program: Program, filename: String): Boolean = {
    writer("\n" + PrettyPrinter.print(program))
    true
  }

  override def draw(filename: String, drawables: List[Drawable]): Unit = {
    printer(":draw is not yet supported on web")
  }
}

object ConstructWebGREPL {

  def main(args: Array[String]): Unit = {
    println("Hello world! Welcome to Construct!")
  }
}
