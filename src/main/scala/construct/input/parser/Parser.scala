package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

object ConstructParser extends JavaTokenParsers with PackratParsers {

    // parsing interface
    def apply(s: String): ParseResult[Program] = parseAll(program, s)

    lazy val id: PackratParser[Identifier] =
      (ident ^^ {s => Identifier(s)})

    lazy val state_sep: PackratParser[String] =
      sys.props("line.separator") | ";"

    lazy val program: PackratParser[Program] =
      (rep1sep(statment,rep1(state_sep))~(state_sep.*)^^ {case list~tail => Program(list)})

    lazy val statment: PackratParser[Statement] =
      (let | constructor | intersection)

    lazy val intersection: PackratParser[IntersectionStatement] =
      ("let"~id~"and"~id~"intersect"~"at"~rep1sep(id,",")
        ^^ {case "let"~id1~"and"~id2~"intersect"~"at"~ids =>
            IntersectionStatement(id1, id2, ids)})

    lazy val let: PackratParser[LetStatement] =
      ("let"~id~"be"~"a"~constructor
        ^^ {case "let"~name~"be"~"a"~cons => LetStatement(name, cons)})

    lazy val constructor: PackratParser[Constructor] =
      (circle | line | point)

    lazy val circle: PackratParser[CircleConstructor] =
      ("circle"~"with"~"center"~id~"and"~"edge"~id
        ^^ {case "circle"~"with"~"center"~c~"and"~"edge"~e => CircleConstructor(c, e)})

    lazy val line: PackratParser[LineConstructor] =
      ("line"~"from"~id~"to"~id
        ^^ {case "line"~"from"~a~"to"~b => LineConstructor(a, b)})

    lazy val point: PackratParser[PointConstructor] =
      ("point" ^^ {case "point" => PointConstructor()})
 }
