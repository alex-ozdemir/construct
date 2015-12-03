package construct.input

import scala.tools.nsc.EvalLoop
import java.awt.image.BufferedImage
import scala.swing._
import construct.input.parser.ConstructParser
import construct.input.loader.Loader
import construct.input.ast._
import construct.semantics.ConstructInterpreter
import construct.semantics.ConstructError
import construct.output._
import construct.engine._

object ConstructGUI extends EvalLoop with App {
  override def prompt = "Construct $ "
  val ui = new UI
  // Default output file
  var outputFile = "out.png"
  ui.visible = true

  var interpreter = new ConstructInterpreter
  var first = true
  var suggestions = List[(List[Drawable],Expr,String)]()
  loop { line =>
    try {
      var redraw = true
      if (line == ":reset" || line == ":r") {
        interpreter = new ConstructInterpreter
        first = true
      }
      else if (
        ConstructParser.parseSuggestionTake(line) match {
          case ConstructParser.Success(_, _) => {
            println("Suggestion take triggered")
            true
          }
          case e: ConstructParser.NoSuccess  => false
        }
      ) {
        ConstructParser.parseSuggestionTake(line) match {
          case ConstructParser.Success((pattern, sug_id), _) => {
            val draw_expr = suggestions find {
              case (drawables, expr, name) => name == sug_id
            }
            draw_expr map {
              case (draw, expr, _) => interpreter.execute(Statement(pattern, expr))
            }
            if (!draw_expr.isDefined) println(s"Suggestion $sug_id not found")
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else if ((line startsWith ":draw") || (line startsWith ":d")) {
        val splitLine = line.split(" +")
        if (splitLine.length > 1) outputFile = splitLine(1)
        PNG.dump(interpreter.get_drawables.toList, outputFile)
      }
      else if ((line startsWith ":suggest") || (line startsWith ":s")) {
        val splitLine = line.split(" +")
        if (splitLine.length > 1) {
          suggestions = interpreter.query(Identifier(splitLine(1)))
          val tmp_drawables = suggestions flatMap {
            case (drawables, _, _) => drawables
          }
          ui.setImage(PNG.getTmp(interpreter.get_drawables.toList, tmp_drawables))
          redraw = false
        }
      }
      else if (line startsWith "include") {
        ConstructParser.parseInclude(line) match {
          case ConstructParser.Success(Path(p), _) => {
            val (item_map, cons) = Loader(p)
            interpreter.add_items(item_map.values.toList)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else if (first) {
        first = false
        ConstructParser.parseGivens(line) match {
          case ConstructParser.Success(t, _) => {
            interpreter.inputs(t, None)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      else {
        ConstructParser.parseStatement(line) match {
          case ConstructParser.Success(t, _) => {
            interpreter.execute(t)
          }
          case e: ConstructParser.NoSuccess  => println(e)
        }
      }
      if (redraw) ui.setImage(PNG.get(interpreter.get_drawables.toList))
    }
    catch {
      case e: ConstructError => println(e)
    }
  }
  ui.dispose()
}


class ImagePanel extends Panel
{
  private var _bufferedImage: BufferedImage = null

  def setImage(value: BufferedImage) =
  {
    _bufferedImage = value
  }

  override def paintComponent(g:Graphics2D) =
  {
    if (null != _bufferedImage) g.drawImage(_bufferedImage, 0, 0, null)
  }
}

class UI extends MainFrame {
  title = "Construct"
  preferredSize = new Dimension(500, 500)
  private val image = new ImagePanel()
  contents = image
  def setImage(img: BufferedImage) = {
    image.setImage(img)
    repaint()
  }
}
