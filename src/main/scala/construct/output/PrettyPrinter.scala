// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This file holds a pretty printer for Construct

package construct.output

import construct.input.ast._
object PrettyPrinter {
  val s = " "
  val c = ", "
  val n = sys.props("line.separator")
  def print(p: Program) : String = printPaths(p.references) + printItems(p.constructions)
  def print(c: Construction, ty: String) : String = {
    val Construction(name, params, statements, returns) = c
    ty + s + print(name) + n + printParams(params) +
      printStatements(statements) + printReturns(returns)
  }
  def printPaths(ps: List[Path]) : String = (ps map { print(_) } mkString n) + n
  def print(p: Path) : String = "include" + s + p.path
  def printItems(is: List[Item]) : String = (is map { print(_) } mkString n) + n
  def print(i: Item) : String =
    i match {
      case Shape(c) => print(c, "shape")
      case c: Construction => print(c, "construction")
    }
  def printParams(ps: List[Parameter]) : String =
    "given" + s + (ps map { print(_) } mkString c) + n
  def print(p: Parameter) : String = {
    val Parameter(name, ty) = p
    print(ty) + s + print(name)
  }
  def print(id: Identifier) : String = id.name
  def printStatements(sts: List[Statement]) : String = (sts map { print(_) } mkString n) + n
  def print(st: Statement) : String = {
    val Statement(pattern, expr) = st
    "let" + s + print(pattern) + s + "=" + s + print(expr)
  }
  def print(p: Pattern) : String =
    p match {
      case Destructor(ty, contents) => print(ty) + print(Tuple(contents))
      case construct.input.ast.Id(id) => print(id)
      case Tuple(contents) => "(" + (contents map { print(_) } mkString c) + ")"
    }
  def print(e: Expr) : String =
    e match {
      case FnApp(fn, args) => print(fn) + "(" + (args map { print(_) } mkString c) + ")"
      case Exactly(id) => print(id)
      case SetLit(items) => "{" + (items map {print(_)} mkString c) + "}"
      case Difference(left, right) => print(left) + " - " + print(right)
    }
  def printReturns(rets: List[Identifier]) : String =
    "return" + s + printIds(rets) + n

  def printIds(rets: TraversableOnce[Identifier]) : String =
    rets map { print(_) } mkString c
}
