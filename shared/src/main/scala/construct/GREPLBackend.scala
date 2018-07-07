package construct

trait  GREPLBackend {
  def processLine(line: String): Unit
  def processPointClick(pt: engine.Point): Unit
  var helpMessage: String
}
