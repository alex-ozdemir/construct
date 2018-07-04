package construct.input

import construct.input.ast._
import construct.input.parser.ConstructParser
import org.scalatest.Matchers
import org.scalatest.FunSuite

class TestParser extends FunSuite with Matchers {

  def identExpr(s: String): Expr = Exactly(Identifier(s))

  test("Identifier") {
    val input = "A"
    ConstructParser.parseAll(ConstructParser.expr, input).get should be(
      Exactly(Identifier("A")))
  }

  test("Function parse") {
    val input = "fn(A, B)"
    ConstructParser.parseAll(ConstructParser.expr, input).get should be(
      FnApp(Identifier("fn"),
            List(Exactly(Identifier("A")), Exactly(Identifier("B")))))
  }

  test("Simple Assignment") {
    val input = "let C = fn(A, B)"
    ConstructParser.parseAll(ConstructParser.statement, input).get should be(
      Statement(Id(Identifier("C")),
                FnApp(Identifier("fn"),
                      List(Exactly(Identifier("A")),
                           Exactly(Identifier("B"))))))
  }

  test("Tuple Pattern") {
    val input = "let (C, D) = fn(A, B)"
    ConstructParser.parseAll(ConstructParser.statement, input).get should be(
      Statement(Tuple(List(Id(Identifier("C")), Id(Identifier("D")))),
                FnApp(Identifier("fn"),
                      List(Exactly(Identifier("A")),
                           Exactly(Identifier("B"))))))
  }

  test("Destructor Pattern") {
    val input = "let seg(C, D) = fn(A, B)"
    ConstructParser.parseAll(ConstructParser.statement, input).get should be(
      Statement(
        Destructor(Identifier("seg"),
                   List(Id(Identifier("C")), Id(Identifier("D")))),
        FnApp(Identifier("fn"),
              List(Exactly(Identifier("A")), Exactly(Identifier("B"))))
      ))
  }

  test("Set Literal") {
    val input = "let C = intersection(A, C) - {D}"
    ConstructParser.parseGREPLInstruction(input).get should be(
      Statement(
        Id(Identifier("C")),
        Difference(FnApp(Identifier("intersection"),
                         List(Exactly(Identifier("A")),
                              Exactly(Identifier("C")))),
                   SetLit(List(Exactly(Identifier("D")))))
      ))
  }
}
