package construct.grepl

trait  GREPLBackend {
  def processEvent(event: UserEvent): Unit
  var helpMessage: String
}
