package construct

import java.awt.image.BufferedImage
import java.io.{FileWriter, PrintWriter}

import scala.tools.nsc.EvalLoop
import scala.swing._
import construct.engine.Point
import construct.input.ast.Program
import construct.input.loader.{FileSystemLoader, Loader}
import construct.output.{Drawable, PNG, PrettyPrinter}
import construct.semantics.ConstructError

object ConstructCLIGREPL extends App with EvalLoop with GREPLFrontend {
  override def prompt = "Construct $ "

  var pixelsToPoints: swing.Point => Option[engine.Point] = { _ =>
    Some(Point(0.0, 0.0))
  }

  var grepl: GREPLBackend = _

  // Initialize UI
  val ui = new UI(pixelsToPoints(_) foreach { grepl.processPointClick })
  ui.visible = true

  val loader: Loader = new FileSystemLoader
  grepl = new GREPLBackendImpl(this, loader)

  override def printToShell(msg: String): Unit = {
    println(msg)
  }

  override def drawToScreen(shapes: List[Drawable],
                            suggestions: List[List[Drawable]]): Unit = {
    val (image, revereseHomography) = PNG.getTmp(shapes, suggestions)
    ui.setImage(image)
    ui.repaint()
    pixelsToPoints = { pt =>
      Some(revereseHomography(Point(pt.x, pt.y)))
    }
  }

  override def draw(filename: String, drawables: List[Drawable]): Unit = {
    PNG.dump(drawables, filename)
  }

  override def write(program: Program, filename: String): Boolean = {
    val programStr = PrettyPrinter.print(program)
    new PrintWriter(new FileWriter(filename, true)) {
      write(programStr)
      close()
    }
    true
  }

  // =============== //
  // The actual loop //
  // =============== //

  loop { line =>
    try {
      grepl.processLine(line)
    } catch {
      case e: ConstructError => printToShell(e.fullMsg)
    }
  }

  // Teardown the UI
  ui.dispose()
}

class ImagePanel extends Panel {
  private var _bufferedImage: BufferedImage = _

  def setImage(value: BufferedImage): Unit = {
    _bufferedImage = value
  }

  override def paintComponent(g: Graphics2D): Unit = {
    if (null != _bufferedImage) g.drawImage(_bufferedImage, 0, 0, null)
  }
}

class UI(onclick: java.awt.Point => Unit) extends MainFrame {
  title = "Construct GREPL"
  preferredSize = new Dimension(500, 500)
  private val image = new ImagePanel()
  contents = image
  def setImage(img: BufferedImage): Unit = {
    image.setImage(img)
    repaint()
  }
  listenTo(image.mouse.clicks)
  reactions += {
    case event.MouseClicked(_, p, _, _, _) => onclick(p)
  }
}
