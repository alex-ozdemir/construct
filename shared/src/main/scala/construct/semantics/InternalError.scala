package construct.semantics

import construct.input.ast.Pattern

/**
  * Some errors are usefull internally, but should never be shown to the user.
  * One example is errors that are incomplete.
  *
  * `InternalError` does not extend Throwable to encourage closer error handling.
  */
sealed abstract class InternalError

object InternalError {
  sealed abstract class BuiltinBindError extends InternalError
  case class BuiltinBindValueError(builtin: Builtins.Type, subpats: List[Pattern], value: Value) extends BuiltinBindError
  case class BuiltinBindArityError(builtin: Builtins.Type, i: Int) extends BuiltinBindError
}
