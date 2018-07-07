package construct

import construct.input.ast.Program
import construct.output.Drawable

trait GREPLFrontend {
  def printToShell(msg: String): Unit
  def drawToScreen(shapes: List[Drawable], suggestions: List[List[Drawable]]): Unit
  def draw(filename: String, drawables: List[Drawable])

  /**
    * Write this program to this file. Return whether the write was successful
    * @param program the program to write
    * @param filename the file to save it in
    * @return
    */
  def write(program: Program, filename: String): Boolean
}
