package construct.semantics

import construct.input.ast._
import construct.semantics.Value._
import construct.output.PrettyPrinter

import scala.util.parsing.input.Position

abstract class ConstructError(val msg: String, val position: Option[Position])
    extends RuntimeException(s"Error: $msg") {
  val fullMsg: String = {
    position map { pos =>
      val lineStr = s"${pos.line}"
      val src = pos.longString.split("\n") zip List(
        lineStr + "| ",
        (" " * lineStr.length) + "| ") map {
        case (r, l) => l + r
      } mkString "\n"
      s"Error: $msg\n$src"
    } getOrElse s"Error: $msg"
  }
}

object ConstructError {

  case class UnknownIdentifier(id: Identifier)
      extends ConstructError(s"Unknown identifier `${id.name}`", Some(id.pos))

  case class UnknownIdentifierOfType(id: Identifier, ty: String)
      extends ConstructError(s"Unknown identifier `${id.name}` of type `$ty`",
                             Some(id.pos))

  case class TypeError(v: Value, expected: String)
      extends ConstructError(s"<${v.pretty}> was expected to be a `$expected`!",
                             None)

  case class ParameterTypeError(param: Parameter, v: Value)
      extends ConstructError(
        s"The formal parameter `${param.name.name}` is supposed to have type `${param.ty}` but the actual parameter was <${v.pretty}>!",
        None)

  case class UsedIdentifier(id: Identifier)
      extends ConstructError(
        s"The identifier `${id.name}` has already been used",
        Some(id.pos))

  case class SelfIntersection(fn: FnApp, v: Value)
      extends ConstructError(s"The function application `${PrettyPrinter.print(
        fn)}` is trying to intersect <${v.pretty}> with itself", Some(fn.pos))

  case class BuiltinMisuse(ty: String, from: String)
      extends ConstructError(s"Cannot construct a `$ty` from $from", None)

  case class ImplicitGiven(param: Parameter)
      extends ConstructError(
        s"Implicit givens must be points, but `${PrettyPrinter
          .print(param.name)}` is a `${param.ty}`",
        None)

  case class Rebind(pattern: Pattern, ident: Identifier)
      extends ConstructError(
        s"The pattern `${PrettyPrinter.print(pattern)}` rebinds the variable `${ident.name}`",
        Some(pattern.pos))

  case class Arity(c: Construction, vars: List[Value])
      extends ConstructError(
        s"The construction `${c.name.name}` expects ${c.parameters.length} parameters but got ${vars.length}",
        None)

  case class BuiltinArity(b: Builtins.Function, vars: List[Value])
      extends ConstructError(
        s"The builtin `${b.name}` expects ${b.arity} parameters but got ${vars.length}",
        None)

  case class BuiltinBindArityError(b: Builtins.Type, arity: Int, pat: Pattern)
      extends ConstructError(
        s"The builtin `${b.name}` cannot be deconstructed into $arity objects",
        Some(pat.pos))

  case class BuiltinBindValueError(b: Builtins.Type, value: Value, pat: Pattern)
      extends ConstructError(
        s"The value <${value.pretty}> cannot be deconstructed into a ${b.name}",
        Some(pat.pos))

  abstract class BindError(value_ty: String,
                           v: Value,
                           p: Pattern,
                           reason: String)
      extends ConstructError(
        s"Cannot bind the $value_ty <${v.pretty}> to the pattern `${PrettyPrinter
          .print(p)}. $reason",
        Some(p.pos)
      )

  case class ProductBindError(union: Tuple, prod: Product)
      extends BindError("ordered union",
                        prod,
                        union,
                        "The number of loci disagree")

  case class DestructorBindArityError(des: Destructor, v: Value)
      extends BindError("value", v, des, "The arities disagree")

  case class DestructorBindTypeError(des: Destructor, v: Value)
      extends BindError("value", v, des, "The types disagree")

  case class VagueBindError(pat: Pattern, v: Value)
      extends BindError("value", v, pat, "")

  case class FileNotFound(error: String) extends ConstructError(error, None)

  case class IncludeError(filename: String, error: String)
      extends ConstructError(
        s"While reading '$filename' I encountered the following error:\n$error",
        None)

  case class WebInclude(file: String)
      extends ConstructError(
        s"I cannot include `$file`, as web construct does not support includes",
        None)

  case class InvalidDifference(left: Value, right: Value)
      extends ConstructError(
        s"Cannot compute <${left.pretty}> - <${right.pretty}>",
        None)

}
